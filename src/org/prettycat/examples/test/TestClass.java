package org.prettycat.examples.test;

import java.lang.NullPointerException;

public class TestClass {
	
	int k;
	
	public void call(int i) {
		i = i + k;
		if (i > 0) { 
			i = method1(i);
		} else {
			i = AnotherTestClass.method2(i);
		}
		System.out.println(i);
	}
	
	public int exceptionTest(int i) {
		try {
			method1(i);
		} catch (NullPointerException e) {
			AnotherTestClass.method2(i);
		}
		return i+1;
	}
	
	public void phi(int k) {
		int j;
		switch (k) {
		case -1:
			j = 23;
			break;
		case 0:
			j = simpleReturn(k);
			break;
		case 1:
			j = 12;
			break;
		case 2:
			j = 274;
			break;
		default:
			j = 2;
		}
		call(j);
	}
	
	public void loop(int k) {
		for (int j = 1; j < k; j++) {
			method1(j);
		}
	}
	
	public int simpleReturn(int i) {
		return i + 1;
	}

	public int method1(int i) {
		return AnotherTestClass.method2(-i % k);
	}
}
