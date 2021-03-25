package cn.yjh.entity;

/**
 * @description:
 * @author: You Jinhua
 * @create: 2021-03-01 10:07
 */
public class Green {
	public Green(){
		System.out.println("对象生命周期------构造方法\n");
	}

	public void init(){
		System.out.println("对象生命周期------初始化方法\n");
	}

	public void destroy(){
		System.out.println("对象生命周期------销毁方法\n");
	}
}
