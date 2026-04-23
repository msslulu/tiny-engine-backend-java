package com.tinyengine.it.dynamic.util;

public class SQLIdentifierValidator {

	private static final String IDENTIFIER_REGEX = "^[a-zA-Z_][a-zA-Z0-9_]*$";

	/**
	 * Validates a SQL identifier (e.g., table name, column name).
	 *
	 * @param identifier the identifier to validate
	 * @return true if valid, false otherwise
	 */
	public static boolean isValidIdentifier(String identifier) {
		if (identifier == null || identifier.trim().isEmpty()) {
			return false;
		}
		return identifier.matches(IDENTIFIER_REGEX);
	}

	/**
	 * Validates a list of SQL identifiers.
	 *
	 * @param identifiers the list of identifiers to validate
	 * @throws IllegalArgumentException if any identifier is invalid
	 */
	public static void validateIdentifiers(Iterable<String> identifiers) {
		if (identifiers != null) {
			for (String identifier : identifiers) {
				if (!isValidIdentifier(identifier)) {
					throw new IllegalArgumentException("Invalid SQL identifier: " + identifier);
				}
			}
		}
	}
}
