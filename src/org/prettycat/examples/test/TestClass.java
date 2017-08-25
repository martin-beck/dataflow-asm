package org.prettycat.examples.test;

import java.lang.NullPointerException;

public class TestClass {
	
	int k;
	
	public void call(int i) {
		i = i + k;
		if (i > 0) { 
			method1(i);
		} else {
			method2(i);
		}
	}
	
	public int exceptionTest(int i) {
		try {
			method1(i);
		} catch (NullPointerException e) {
			method2(i);
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
			j = 1;
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
		method1(j);
	}

	public void method1(int i) {
		method2(-i % k);
	}
	
	public void method2(int i) {
		i -= k;
		System.out.println(i);
	}
}
