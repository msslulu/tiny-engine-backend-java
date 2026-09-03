package com.tinyengine.it.common.utils;

import java.util.List;

public class SqlIdentifierValidator {

    private SqlIdentifierValidator() {
    }

    public static void validate(String identifier) {
        if (!isValidIdentifier(identifier)) {
            throw new IllegalArgumentException("Invalid SQL identifier: " + identifier);
        }
    }

    public static String requireValidIdentifier(String identifier) {
        validate(identifier);
        return identifier;
    }

    public static void validateAll(List<String> identifiers) {
        if (identifiers == null) {
            return;
        }
        identifiers.forEach(SqlIdentifierValidator::validate);
    }

    public static void validateOrderType(String orderType) {
        if (!isValidOrderType(orderType)) {
            throw new IllegalArgumentException("Invalid order type: " + orderType);
        }
    }

    public static String requireValidOrderType(String orderType) {
        validateOrderType(orderType);
        return orderType.toUpperCase(java.util.Locale.ROOT);
    }

    public static boolean isValidIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return false;
        }

        if (!isIdentifierStart(identifier.charAt(0))) {
            return false;
        }

        for (int i = 1; i < identifier.length(); i++) {
            if (!isIdentifierPart(identifier.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidOrderType(String orderType) {
        return "ASC".equalsIgnoreCase(orderType) || "DESC".equalsIgnoreCase(orderType);
    }

    public static String escapeSqlLiteral(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString()
                .replace("\\", "\\\\")
                .replace("'", "''");
    }

    private static boolean isIdentifierStart(char c) {
        return c == '_' || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private static boolean isIdentifierPart(char c) {
        return isIdentifierStart(c) || (c >= '0' && c <= '9');
    }
}
