package io.quarkiverse.quickjs4j.runtime;

import static java.lang.String.format;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import io.quarkiverse.quickjs4j.ScriptInterfaceFactory;
import io.quarkiverse.quickjs4j.annotations.ScriptImplementation;
import io.quarkiverse.quickjs4j.util.ScriptInterfaceUtils;
import io.roastedroot.quickjs4j.annotations.ScriptInterface;

public class ScriptInterfaceProcessor extends AbstractProcessor {

    static PackageElement getPackageName(Element element) {
        Element enclosing = element;
        while (enclosing.getKind() != ElementKind.PACKAGE) {
            enclosing = enclosing.getEnclosingElement();
        }
        return (PackageElement) enclosing;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    Filer filer() {
        return processingEnv.getFiler();
    }

    Elements elements() {
        return processingEnv.getElementUtils();
    }

    void log(Diagnostic.Kind kind, String message, Element element) {
        processingEnv.getMessager().printMessage(kind, message, element);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(ScriptInterface.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(ScriptInterface.class)) {
            generateFactoryFor((TypeElement) element);

            ScriptImplementation scriptImplementationAnnotation = element.getAnnotation(ScriptImplementation.class);
            if (scriptImplementationAnnotation != null) {
                generateCdiBeanFor((TypeElement) element, scriptImplementationAnnotation.location());
            }
        }

        return false;
    }

    private void generateCdiBeanFor(TypeElement scriptInterfaceElement, String scriptLocation) {
        String packageName = getPackageName(scriptInterfaceElement).toString();
        String scriptInterfaceName = scriptInterfaceElement.getSimpleName().toString();
        String scriptInterfaceFQN = packageName + "." + scriptInterfaceName;
        String cdiBeanClassName = scriptInterfaceName + "_CDI";
        String cdiBeanClassFQN = packageName + "." + cdiBeanClassName;
        String proxyClassName = scriptInterfaceElement.getSimpleName().toString() + "_Proxy";
        String proxyClassFQN = packageName + "." + proxyClassName;

        AnnotationMirror scriptInterfaceAnnotation = getScriptInterfaceAnnotation(scriptInterfaceElement);
        Element contextClass = getContextClassFromAnnotation(scriptInterfaceAnnotation);

        boolean hasContextClass = contextClass != null;
        String contextClassName = "Void";

        // Create the CDI Bean class and add all imports
        JavaClassSource cdiBeanSource = Roaster.create(JavaClassSource.class);
        cdiBeanSource.setPackage(packageName);
        cdiBeanSource.setName(cdiBeanClassName);
        cdiBeanSource.addImport(ApplicationScoped.class);
        cdiBeanSource.addImport(scriptInterfaceFQN);
        cdiBeanSource.addImport(proxyClassFQN);
        cdiBeanSource.addImport(ScriptInterfaceUtils.class);
        cdiBeanSource.addAnnotation(ApplicationScoped.class);

        if (hasContextClass) {
            String contextClassPackage = getPackageName(contextClass).toString();
            contextClassName = contextClass.getSimpleName().toString();
            String contextClassFQN = contextClassPackage + "." + contextClassName;

            cdiBeanSource.addImport(contextClassFQN);
            cdiBeanSource.addImport(Inject.class);
        }

        // The CDI bean class implements the script interface
        cdiBeanSource.addInterface(scriptInterfaceName);

        // Create the SCRIPT_LIBRARY_LOCATION static field
        cdiBeanSource.addField()
                .setPrivate()
                .setStatic(true)
                .setType(String.class)
                .setName("SCRIPT_LIBRARY_LOCATION")
                .setLiteralInitializer(String.format("\"%s\"", scriptLocation));

        // Create the context field (if needed)
        if (hasContextClass) {
            cdiBeanSource.addField()
                    .setType(contextClassName)
                    .setName("context")
                    .addAnnotation(Inject.class);
        }

        // Create the _createDelegate() method
        String createDelegateMethodBody = """
                    String scriptLibrary = ScriptInterfaceUtils.loadScriptLibrary(SCRIPT_LIBRARY_LOCATION);
                    return new PROXY_CLASS_NAME(scriptLibrary, context);
                """;
        if (!hasContextClass) {
            createDelegateMethodBody = """
                        String scriptLibrary = ScriptInterfaceUtils.loadScriptLibrary(SCRIPT_LIBRARY_LOCATION);
                        return new PROXY_CLASS_NAME(scriptLibrary);
                    """;
        }
        MethodSource<JavaClassSource> createDelegateMethodSource = cdiBeanSource.addMethod();
        createDelegateMethodSource.setPrivate();
        createDelegateMethodSource.setReturnType(proxyClassName);
        createDelegateMethodSource.setName("_createDelegate");
        createDelegateMethodSource.setBody(template(createDelegateMethodBody, Map.of(
                "PROXY_CLASS_NAME", proxyClassName)));

        // Implement all methods in the script interface
        Set<String> excludedMembers = Set.of(scriptInterfaceElement.getAnnotation(ScriptInterface.class).excluded());
        for (Element member : elements().getAllMembers(scriptInterfaceElement)) {
            if (member.getKind() == ElementKind.METHOD
                    && member instanceof ExecutableElement
                    && !excludedMembers.contains(member.getSimpleName().toString())) {

                String methodName = member.getSimpleName().toString();
                MethodSource<JavaClassSource> methodSource = cdiBeanSource.addMethod();
                methodSource.setPublic();
                methodSource.setName(methodName);
                methodSource.addAnnotation(Override.class);

                List<String> paramNames = new ArrayList<>(((ExecutableElement) member).getParameters().size());
                for (VariableElement parameter : ((ExecutableElement) member).getParameters()) {
                    String paramName = parameter.getSimpleName().toString();
                    String paramType = parameter.asType().toString();
                    if (paramType.contains(".")) {
                        String paramTypeFQN = paramType;
                        String paramTypeName = paramTypeFQN.substring(paramTypeFQN.lastIndexOf('.') + 1);
                        methodSource.addParameter(paramTypeName, paramName);
                        cdiBeanSource.addImport(paramTypeFQN);
                    } else {
                        methodSource.addParameter(paramType, paramName);
                    }
                    paramNames.add(paramName);
                }

                String returnType = ((ExecutableElement) member).getReturnType().toString();
                if (returnType.contains(".")) {
                    String returnTypeFQN = returnType;
                    String returnTypeName = returnTypeFQN.substring(returnTypeFQN.lastIndexOf('.') + 1);
                    methodSource.setReturnType(returnTypeName);
                    cdiBeanSource.addImport(returnTypeFQN);
                } else {
                    methodSource.setReturnType(returnType);
                }

                List<? extends TypeMirror> thrownTypes = ((ExecutableElement) member).getThrownTypes();
                for (TypeMirror thrownType : thrownTypes) {
                    methodSource.addThrows(thrownType.toString());
                }

                String methodBody = """
                            try (var delegate = _createDelegate()) {
                                RETURN delegate.METHOD_NAME(METHOD_ARGS);
                            }
                        """;
                methodSource.setBody(template(methodBody, Map.of(
                        "RETURN", "void".equals(returnType) ? "" : "return",
                        "METHOD_NAME", methodName,
                        "METHOD_ARGS", String.join(",", paramNames))));
            }
        }

        try (Writer writer = filer().createSourceFile(cdiBeanClassFQN, scriptInterfaceElement).openWriter()) {
            writer.write(cdiBeanSource.toString());
        } catch (IOException e) {
            log(ERROR, format("Failed to create %s file: %s", cdiBeanClassFQN, e), null);
        }
    }

    private void generateFactoryFor(TypeElement scriptInterfaceElement) {
        log(NOTE, "Generating factory CDI bean class for script interface: " + scriptInterfaceElement, null);

        String packageName = getPackageName(scriptInterfaceElement).toString();
        String scriptInterfaceName = scriptInterfaceElement.getSimpleName().toString();
        String scriptInterfaceFQN = packageName + "." + scriptInterfaceName;
        String factoryClassName = scriptInterfaceName + "_Factory";
        String factoryClassFQN = packageName + "." + factoryClassName;
        String proxyClassName = scriptInterfaceElement.getSimpleName().toString() + "_Proxy";
        String proxyClassFQN = packageName + "." + proxyClassName;

        AnnotationMirror scriptInterfaceAnnotation = getScriptInterfaceAnnotation(scriptInterfaceElement);
        Element contextClass = getContextClassFromAnnotation(scriptInterfaceAnnotation);

        boolean hasContextClass = contextClass != null;
        String contextClassName = "Void";

        // Create the factory class and add all imports
        JavaClassSource factorySource = Roaster.create(JavaClassSource.class);
        factorySource.setPackage(packageName);
        factorySource.setName(factoryClassName);
        factorySource.addImport(ApplicationScoped.class);
        factorySource.addImport(ScriptInterfaceFactory.class);
        factorySource.addImport(scriptInterfaceFQN);
        factorySource.addImport(proxyClassFQN);

        factorySource.addAnnotation(ApplicationScoped.class);

        if (hasContextClass) {
            String contextClassPackage = getPackageName(contextClass).toString();
            contextClassName = contextClass.getSimpleName().toString();
            String contextClassFQN = contextClassPackage + "." + contextClassName;

            factorySource.addImport(contextClassFQN);
        }

        // The factory class implements ScriptInterfaceFactory<T, C>
        factorySource.addInterface(template("INTERFACE<TYPE, CONTEXT>", Map.of(
                "INTERFACE", ScriptInterfaceFactory.class.getSimpleName(),
                "TYPE", scriptInterfaceName,
                "CONTEXT", contextClassName)));

        // Create the create(String, Context) method
        String produceMethodBody = """
                    return new PROXY_CLASS_NAME(scriptLibrary, context);
                """;
        if (!hasContextClass) {
            produceMethodBody = """
                        return new PROXY_CLASS_NAME(scriptLibrary);
                    """;
        }
        MethodSource<JavaClassSource> createMethodSource = factorySource.addMethod();
        createMethodSource.setPublic();
        createMethodSource.setReturnType(scriptInterfaceName);
        createMethodSource.setName("create");
        createMethodSource.addParameter("String", "scriptLibrary");
        createMethodSource.addParameter(contextClassName, "context");
        createMethodSource.setBody(template(produceMethodBody, Map.of(
                "PROXY_CLASS_NAME", proxyClassName)));
        createMethodSource.addAnnotation(Override.class);

        try (Writer writer = filer().createSourceFile(factoryClassFQN, scriptInterfaceElement).openWriter()) {
            writer.write(factorySource.toString());
        } catch (IOException e) {
            log(ERROR, format("Failed to create %s file: %s", factoryClassFQN, e), null);
        }
    }

    private static AnnotationMirror getScriptInterfaceAnnotation(TypeElement element) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
        for (AnnotationMirror annotationMirror : annotationMirrors) {
            DeclaredType annotationType = annotationMirror.getAnnotationType();
            Element annotationElement = annotationType.asElement();
            String annotationQualifiedName = getPackageName(annotationElement).toString() + "."
                    + annotationElement.getSimpleName();
            if (annotationQualifiedName.equals(ScriptInterface.class.getName())) {
                return annotationMirror;
            }
        }
        return null;
    }

    private static Element getContextClassFromAnnotation(AnnotationMirror scriptInterfaceAnnotation) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> values = scriptInterfaceAnnotation.getElementValues();
        Set<? extends Map.Entry<? extends ExecutableElement, ? extends AnnotationValue>> entries = values.entrySet();
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : entries) {
            String name = entry.getKey().getSimpleName().toString();
            if (name.equals("context")) {
                AnnotationValue value = entry.getValue();
                TypeMirror typeMirror = (TypeMirror) value.getValue();
                if (typeMirror.getKind() == TypeKind.DECLARED) {
                    DeclaredType declaredType = (DeclaredType) typeMirror;
                    TypeElement typeElement = (TypeElement) declaredType.asElement();
                    return typeElement;
                }
            }
        }
        return null;
    }

    private static String template(String templateSource, Map<String, String> templateParams) {
        String rval = templateSource;
        Set<Map.Entry<String, String>> entries = templateParams.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();
            rval = rval.replaceAll(key, value);
        }
        return rval;
    }
}
