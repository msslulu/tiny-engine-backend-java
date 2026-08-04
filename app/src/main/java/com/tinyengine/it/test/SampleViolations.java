package com.tinyengine.it.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SampleViolations {
	// 1. 未使用的私有字段 (UnusedPrivateField)
	private String unusedField = "I am not used";

	// 2. 常量命名不符合规范 (ConstantNamingConventions) → 应全大写
	public static final String myConstant = "should be UPPER_CASE";

	// 3. 字段命名不符合规范 (FieldNamingConventions) → 应 camelCase，这里合规，略过

	// 4. 不应使用 'l' 作为变量名 (AvoidFieldNameMatchingMethodName) 但更常见的是短变量名

	public void demoMethod() {
		// 5. 未使用的局部变量 (UnusedLocalVariable)
		int unusedLocal = 42;

		// 6. 短变量名 (ShortVariable)
		int a = 10;

		// 7. 使用 System.out.println (SystemPrintln)
		System.out.println("This is a direct system print");

		// 8. 魔法数字 (MagicNumber)
		int magic = (int) (3.14 * 2); // 3.14 和 2 都是魔法数字

		// 9. 在循环中创建对象 (AvoidInstantiatingObjectsInLoops)
		List<String> list = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			String s = new String("loop object"); // 无必要
			list.add(s);
		}

		// 10. 空的 catch 块 (EmptyCatchBlock)
		try {
			Files.readAllLines(Paths.get("nonexistent.txt"));
		} catch (IOException e) {
			// empty
		}

		// 11. 未关闭的资源 (CloseResource)
		try {
			Connection conn = DriverManager.getConnection("jdbc:hsqldb:mem:test", "sa", "");
			// 使用 conn 后未关闭
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// 12. 使用 'return' 语句过多 (MultipleReturns) 或过于复杂
		if (magic > 0) {
			return;
		} else {
			// 多个 return 点
		}

		// 13. 未使用的参数 (UnusedFormalParameter)
		unusedMethod("hello", 123);
	}

	private void unusedMethod(String param1, int param2) {
		// 这里只使用了 param1，param2 未使用
		System.out.println(param1);
	}

	// 14. 方法过长 (TooLongMethod) 但这里仅作演示，可通过增加行数制造，但不必
	// 15. 参数过多 (TooManyParameters)
	public void tooManyParams(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10) {
		// 10个参数，可能触发参数过多规则
	}

	// 16. 不应使用 'Thread.sleep' 在循环中？不强制

	// 17. 使用 'ConcurrentHashMap' 代替 'Hashtable'？不强制

	// 18. 使用 'BigDecimal' 构造函数时避免 double (BigDecimalConstructor)
	public void bigDecimalIssue() {
		java.math.BigDecimal bd = new java.math.BigDecimal(0.1); // 可能触发
	}

	// 19. 使用 'StringBuffer' 而不是 'StringBuilder'？ (StringBufferUsage)
	public void stringBufferUsage() {
		StringBuffer sb = new StringBuffer(); // 可能建议使用 StringBuilder
		sb.append("test");
	}

}
