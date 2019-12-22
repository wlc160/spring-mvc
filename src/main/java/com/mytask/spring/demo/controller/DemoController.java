package com.mytask.spring.demo.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mytask.spring.demo.service.IDemoService;
import com.mytask.spring.mvcframework.annotation.MyAutowired;
import com.mytask.spring.mvcframework.annotation.MyController;
import com.mytask.spring.mvcframework.annotation.MyRequestMapping;
import com.mytask.spring.mvcframework.annotation.MyRequestParam;

@MyController
@MyRequestMapping("/demo")
public class DemoController {

  	@MyAutowired private IDemoService demoService;

	@MyRequestMapping("/query")
	public void query(HttpServletRequest req, HttpServletResponse resp,
					  @MyRequestParam("name") String name){
		String result = demoService.get(name);
//		String result = "My name is " + name;
		try {
			resp.getWriter().write(result);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@MyRequestMapping("/add")
	public void add(HttpServletRequest req, HttpServletResponse resp,
					@MyRequestParam("a") Integer a, @MyRequestParam("b") Integer b){
		try {
			resp.getWriter().write(a + "+" + b + "=" + (a + b));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
