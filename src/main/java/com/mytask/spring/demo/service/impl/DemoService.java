package com.mytask.spring.demo.service.impl;


import com.mytask.spring.demo.service.IDemoService;
import com.mytask.spring.mvcframework.annotation.MyService;

/**
 * 核心业务逻辑
 */
@MyService
public class DemoService implements IDemoService {

	public String get(String name) {
		return "My name is " + name;
	}

}
