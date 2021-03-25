package cn.yjh.controller;

import cn.yjh.config.aop.annotation.ControllerLog;
import cn.yjh.service.InterfaceService;
import cn.yjh.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;

/**
 * @description:
 * @author: You Jinhua
 * @create: 2021-02-21 14:24
 */
@Controller
@Lazy(true)
public class UserController {

	private String name = "张三";

	@Autowired
	private UserService userService;

	@ControllerLog(description = "登录")
	public void query(){
		System.out.println("========查询数据库，获取用户========");
		userService.getUser();
	}


	@RequestMapping("/login")
	@ControllerLog(description = "登录")
	public String login() {
		System.out.println("前往首页！");
		return "index";
	}
}
