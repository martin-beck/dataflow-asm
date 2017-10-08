package org.prettycat.examples.test;

public class AnotherTestClass {

	static int k;
	
	public static int method2(int i) {
		i -= k;
		System.out.println(i);
		return i;
	}
}
