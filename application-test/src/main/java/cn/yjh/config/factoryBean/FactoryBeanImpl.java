package cn.yjh.config.factoryBean;

import cn.yjh.entity.User;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

/**
 * @description: 工厂 Bean 往容器中添加组件，添加的是 getObject()方法的返回值类型
 *   			 如果想从IOC容器中获得FactryBean本身，则需要加取地址符：&
 * @author: You Jinhua
 * @create: 2021-02-28 14:47
 */
//@Component
public class FactoryBeanImpl implements FactoryBean<User> {

	@Override
	public User getObject() throws Exception {
		return new User();
	}

	@Override
	public Class<?> getObjectType() {
		return User.class;
	}

	/**
	 * 是否单例：返回 true 为单例
	 * @return
	 */
	@Override
	public boolean isSingleton() {
		return false;
	}
}
