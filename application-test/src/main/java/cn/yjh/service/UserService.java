package cn.yjh.service;

import cn.yjh.controller.UserController;
import cn.yjh.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @description:
 * @author: You Jinhua
 * @create: 2021-02-21 14:25
 */
@Service
@Lazy(true)
public class UserService implements InterfaceService {

//	@Autowired
//	private UserController userController;

	public User getUser(){
		System.out.println("========查询数据库，获取用户========");
		return new User();
	}
}
