package com.tinyengine.it.common.utils;

public class CheckstyleValidation {
	// This class intentionally violates Checkstyle rules for testing GitHub Action

	public static final int CONSTANT = 42; // Valid constant name

	private int bad_variable_name; // Violates naming convention

	public CheckstyleValidation() {
		// Empty constructor (violates EmptyBlock rule)
	}

	public void badMethod() {
		if (true) {
			System.out.println("This is a bad method"); // Violates line length if too long
		} else {
			return; // SimplifyBooleanReturn violation
		}
	}

	public void multipleVariables() {
		int a = 1, b = 2; // Violates MultipleVariableDeclarations rule
	}

	public void longMethod() { // Violates MethodLength if too long
		for (int i = 0; i < 100; i++) {
			System.out.println("Line " + i);
		}
	}
}
