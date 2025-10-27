package io.quarkiverse.quickjs4j.runtime;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Utility class for mapping Java types to TypeScript types.
 * Used during annotation processing to generate TypeScript definition files.
 */
public class TypeScriptTypeMapper {

    private final Types typeUtils;

    public TypeScriptTypeMapper(Types typeUtils) {
        this.typeUtils = typeUtils;
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

        // Default to any for custom/unknown types
        return "any";
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