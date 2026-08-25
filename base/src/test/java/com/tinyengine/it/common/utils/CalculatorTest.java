package com.tinyengine.it.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CalculatorTest {
	@Test
	void testAdd() {
		Calculator calculator = new Calculator();
		int result = calculator.add(2, 3);
		// 这是一个会通过的测试
		assertEquals(5, result, "2 + 3 应该等于 5");
	}

	@Test
	void testAddWithNegative() {
		Calculator calculator = new Calculator();
		int result = calculator.add(-1, 1);
		// 这是一个会通过的测试
		assertEquals(0, result, "-1 + 1 应该等于 0");
	}
}
// 这是你要测试的简单业务类
class Calculator {
	public int add(int a, int b) {
		return a + b;
	}
}
