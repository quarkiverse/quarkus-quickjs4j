package io.quarkiverse.quickjs4j.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Utility class for mapping Java types to TypeScript types.
 * Used during annotation processing to generate TypeScript definition files.
 * Tracks custom bean types and generates TypeScript interfaces for them.
 */
public class TypeScriptTypeMapper {

    private final Types typeUtils;
    private final Elements elementUtils;
    private final Set<TypeElement> beanTypes;
    private final String scriptInterfacePackagePrefix;

    public TypeScriptTypeMapper(Types typeUtils, Elements elementUtils, String scriptInterfacePackage) {
        this.typeUtils = typeUtils;
        this.elementUtils = elementUtils;
        this.beanTypes = new HashSet<>();
        this.scriptInterfacePackagePrefix = extractPackagePrefix(scriptInterfacePackage);
    }

    /**
     * Maps a Java type to its TypeScript equivalent.
     *
     * @param type the Java type to map
     * @return the TypeScript type as a string
     */
    public String mapType(TypeMirror type) {
        if (type == null) {
            return "any";
        }

        TypeKind kind = type.getKind();

        // Handle primitive types
        if (kind.isPrimitive()) {
            return mapPrimitiveType((PrimitiveType) type);
        }

        // Handle void
        if (kind == TypeKind.VOID) {
            return "void";
        }

        // Handle arrays
        if (kind == TypeKind.ARRAY) {
            ArrayType arrayType = (ArrayType) type;
            String componentType = mapType(arrayType.getComponentType());
            return componentType + "[]";
        }

        // Handle declared types (classes, interfaces)
        if (kind == TypeKind.DECLARED) {
            return mapDeclaredType((DeclaredType) type);
        }

        // Default to any for unknown types
        return "any";
    }

    /**
     * Maps primitive types to TypeScript types.
     */
    private String mapPrimitiveType(PrimitiveType type) {
        return switch (type.getKind()) {
            case BOOLEAN -> "boolean";
            case BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, CHAR -> "number";
            default -> "any";
        };
    }

    /**
     * Maps declared types (classes, interfaces) to TypeScript types.
     */
    private String mapDeclaredType(DeclaredType type) {
        String typeName = type.asElement().toString();

        // Handle common Java types
        if (typeName.equals("java.lang.String")) {
            return "string";
        }

        if (typeName.equals("java.lang.Boolean")) {
            return "boolean";
        }

        if (typeName.equals("java.lang.Integer") ||
                typeName.equals("java.lang.Long") ||
                typeName.equals("java.lang.Short") ||
                typeName.equals("java.lang.Byte") ||
                typeName.equals("java.lang.Float") ||
                typeName.equals("java.lang.Double") ||
                typeName.equals("java.lang.Character")) {
            return "number";
        }

        // Handle List and arrays
        if (typeName.startsWith("java.util.List") ||
                typeName.startsWith("java.util.ArrayList") ||
                typeName.startsWith("java.util.Collection")) {
            if (!type.getTypeArguments().isEmpty()) {
                String elementType = mapType(type.getTypeArguments().get(0));
                return elementType + "[]";
            }
            return "any[]";
        }

        // Handle Set
        if (typeName.startsWith("java.util.Set") ||
                typeName.startsWith("java.util.HashSet")) {
            if (!type.getTypeArguments().isEmpty()) {
                String elementType = mapType(type.getTypeArguments().get(0));
                return elementType + "[]";
            }
            return "any[]";
        }

        // Handle Map
        if (typeName.startsWith("java.util.Map") ||
                typeName.startsWith("java.util.HashMap")) {
            if (type.getTypeArguments().size() >= 2) {
                String keyType = mapType(type.getTypeArguments().get(0));
                String valueType = mapType(type.getTypeArguments().get(1));
                return "Record<" + keyType + ", " + valueType + ">";
            }
            return "Record<string, any>";
        }

        // Handle date/time types as string
        if (typeName.startsWith("java.time.") ||
                typeName.equals("java.util.Date")) {
            return "string";
        }

        // Handle Object
        if (typeName.equals("java.lang.Object")) {
            return "any";
        }

        // Check if this is a custom bean type (not from standard libraries)
        if (isCustomBean(typeName)) {
            TypeElement typeElement = (TypeElement) type.asElement();
            beanTypes.add(typeElement);
            return typeElement.getSimpleName().toString();
        }

        // Default to any for unknown types
        return "any";
    }

    /**
     * Checks if a type name represents a custom bean.
     * A bean is considered custom if it belongs to the same project as the script interface,
     * determined by matching the first two components of the package name.
     *
     * For example:
     * - Bean: io.apicurio.beans.v3.MySimpleBean
     * - Script interface: io.apicurio.converter.Converter
     * - First two components match (io.apicurio), so it's a custom bean.
     *
     * - Bean: io.apicurio.beans.v3.MySimpleBean
     * - Script interface: io.reactive.converter.Converter
     * - First two components don't match, so it's NOT a custom bean.
     */
    private boolean isCustomBean(String typeName) {
        // Extract the first two package components from the bean type
        String beanPackagePrefix = extractPackagePrefix(typeName);

        // Compare with script interface package prefix
        return beanPackagePrefix != null &&
                scriptInterfacePackagePrefix != null &&
                beanPackagePrefix.equals(scriptInterfacePackagePrefix);
    }

    /**
     * Extracts the first two components of a package name.
     * For example:
     * - "io.apicurio.beans.v3.MySimpleBean" returns "io.apicurio"
     * - "com.example.MyClass" returns "com.example"
     * - "MyClass" returns null
     */
    private String extractPackagePrefix(String fullTypeName) {
        if (fullTypeName == null) {
            return null;
        }

        String[] parts = fullTypeName.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        }
        return null;
    }

    /**
     * Gets all bean types that were encountered during type mapping.
     */
    public Set<TypeElement> getEncounteredBeanTypes() {
        return new HashSet<>(beanTypes);
    }

    /**
     * Generates a TypeScript interface definition for a Java bean.
     * Limited to one level depth - complex property types are mapped to 'any'.
     *
     * @param beanType the bean type element
     * @return TypeScript interface definition
     */
    public String generateBeanInterface(TypeElement beanType) {
        StringBuilder ts = new StringBuilder();
        String beanName = beanType.getSimpleName().toString();

        ts.append("export interface ").append(beanName).append(" {\n");

        // Extract properties from getter methods
        List<BeanProperty> properties = extractBeanProperties(beanType);

        for (BeanProperty property : properties) {
            ts.append("    ").append(property.name).append(": ").append(property.tsType).append(";\n");
        }

        ts.append("}\n");

        return ts.toString();
    }

    /**
     * Extracts bean properties from getter methods.
     */
    private List<BeanProperty> extractBeanProperties(TypeElement beanType) {
        List<BeanProperty> properties = new ArrayList<>();
        Set<String> seenProperties = new HashSet<>();

        for (Element member : elementUtils.getAllMembers(beanType)) {
            if (member.getKind() == ElementKind.METHOD && member instanceof ExecutableElement) {
                ExecutableElement method = (ExecutableElement) member;

                // Only look at public methods with no parameters
                if (!method.getModifiers().contains(Modifier.PUBLIC) || !method.getParameters().isEmpty()) {
                    continue;
                }

                String methodName = method.getSimpleName().toString();
                String propertyName = extractPropertyName(methodName);

                // Skip if not a getter or already seen
                if (propertyName == null || seenProperties.contains(propertyName)) {
                    continue;
                }

                // Skip methods from Object class
                if (methodName.equals("getClass")) {
                    continue;
                }

                seenProperties.add(propertyName);

                // Map the return type, but force complex types to 'any' (single-level depth)
                String tsType = mapTypeWithDepthLimit(method.getReturnType());

                properties.add(new BeanProperty(propertyName, tsType));
            }
        }

        return properties;
    }

    /**
     * Extracts property name from getter method name.
     * Returns null if not a valid getter.
     */
    private String extractPropertyName(String methodName) {
        if (methodName.startsWith("get") && methodName.length() > 3) {
            String propertyName = methodName.substring(3);
            return Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            String propertyName = methodName.substring(2);
            return Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
        }
        return null;
    }

    /**
     * Maps a type to TypeScript, but forces custom beans to 'any' (for depth limiting).
     */
    private String mapTypeWithDepthLimit(TypeMirror type) {
        if (type == null) {
            return "any";
        }

        TypeKind kind = type.getKind();

        // Handle primitives and void normally
        if (kind.isPrimitive()) {
            return mapPrimitiveType((PrimitiveType) type);
        }
        if (kind == TypeKind.VOID) {
            return "void";
        }

        // Handle arrays - map component type with depth limit
        if (kind == TypeKind.ARRAY) {
            ArrayType arrayType = (ArrayType) type;
            String componentType = mapTypeWithDepthLimit(arrayType.getComponentType());
            return componentType + "[]";
        }

        // Handle declared types
        if (kind == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            String typeName = declaredType.asElement().toString();

            // Handle standard types normally
            if (typeName.equals("java.lang.String"))
                return "string";
            if (typeName.equals("java.lang.Boolean"))
                return "boolean";
            if (typeName.equals("java.lang.Integer") ||
                    typeName.equals("java.lang.Long") ||
                    typeName.equals("java.lang.Short") ||
                    typeName.equals("java.lang.Byte") ||
                    typeName.equals("java.lang.Float") ||
                    typeName.equals("java.lang.Double") ||
                    typeName.equals("java.lang.Character")) {
                return "number";
            }

            // Handle collections with depth limit
            if (typeName.startsWith("java.util.List") ||
                    typeName.startsWith("java.util.ArrayList") ||
                    typeName.startsWith("java.util.Collection") ||
                    typeName.startsWith("java.util.Set") ||
                    typeName.startsWith("java.util.HashSet")) {
                if (!declaredType.getTypeArguments().isEmpty()) {
                    String elementType = mapTypeWithDepthLimit(declaredType.getTypeArguments().get(0));
                    return elementType + "[]";
                }
                return "any[]";
            }

            // Handle date/time
            if (typeName.startsWith("java.time.") || typeName.equals("java.util.Date")) {
                return "string";
            }

            // Custom beans at this depth level are 'any'
            if (isCustomBean(typeName)) {
                return "any";
            }
        }

        return "any";
    }

    /**
     * Simple holder for bean property information.
     */
    private static class BeanProperty {
        final String name;
        final String tsType;

        BeanProperty(String name, String tsType) {
            this.name = name;
            this.tsType = tsType;
        }
    }

    /**
     * Formats a parameter for TypeScript function signature.
     *
     * @param paramName the parameter name
     * @param paramType the parameter type
     * @return formatted parameter string (e.g., "paramName: string")
     */
    public String formatParameter(String paramName, TypeMirror paramType) {
        return paramName + ": " + mapType(paramType);
    }
}