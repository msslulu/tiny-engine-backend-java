package com.tinyengine.it.common.utils;
import java.lang.reflect.Field;

/**
 * 类名错误：应使用 UpperCamelCase（首字母大写）
 * 这里故意写成了小写开头的 testCheckstyle
 */
public class testCheckstyle {

	// 常量命名错误：应全部大写并用下划线分隔
	public static final String myConstant = "Hello";

	// 成员变量命名错误：不应以下划线开头
	private String _name;

	// 成员变量命名错误：不应使用单个字符
	private int a;

	/**
	 * 构造方法：可以接受参数，但未提供 Javadoc
	 */
	public testCheckstyle(String name) {
		this._name = name;
	}

	public void test() {
		long value = 1l;  // 错误：使用了小写 l
		System.out.println(value);
	}
	public void longMethod() {
		// 填充到超过 300 行，例如添加 301 个空行或打印语句
		for (int i = 0; i < 301; i++) {
			System.out.println("line " + i);
		}
	}

	public void test1() {
		if (true) {
			if (true) {
				if (true) {
					if (true) {  // 第4层嵌套，超过最大值3
						System.out.println("deep");
					}
				}
			}
		}
	}

	public void test2() {
		String s = "hello";
		if (s == "hello") {  // 错误：应使用 equals()
			System.out.println("equal");
		}
	}

	/**
	 * 方法名错误：不应包含大写字母，应使用 lowerCamelCase
	 */
	public void SayHello() {
		// 行长度超限（超过120字符），故意写长
		System.out.println("This line is intentionally made extremely long to exceed the maximum line length limit which is usually set to 120 characters in Huawei coding standard. It definitely exceeds 120 chars.");

		// 缩进可能使用Tab（如果你的编辑器未转换，此处会触发）
		// 未使用的局部变量
		String unused = "I am not used anywhere";
	}

	/**
	 * 公共方法缺少 Javadoc 注释
	 */
	public void doSomething() {
		// 方法体为空或简单，但缺少注释

	}

	/**
	 * 方法名包含下划线，违反 lowerCamelCase
	 */
	public void do_this() {
		// 空方法体
	}

	/**
	 * 参数和返回值缺少 Javadoc 说明（如果有要求）
	 */
	public int add(int a, int b) {
		return a + b;
	}

	/**
	 * 重载方法，但缺少 Javadoc
	 */
	public int add(int a, int b, int c) {
		return a + b + c;
	}

	// 缺少访问修饰符的方法（默认包级别，可能不符合规范）
	void packagePrivateMethod() {
		System.out.println("This method has no access modifier.");
	}

	// 静态方法命名错误
	public static void StaticMethod() {
		// 方法名首字母大写，违反 lowerCamelCase
	}

	// 常量应放在静态代码块或声明时赋值，但此处未遵循顺序
	public static final String ANOTHER_CONST;
	static {
		ANOTHER_CONST = "Init";
	}

	void test3(){
		String json = "{\"key\":\"value\",\"number\":123}";
		byte[] jsonBytes = json.getBytes();
	}
}
