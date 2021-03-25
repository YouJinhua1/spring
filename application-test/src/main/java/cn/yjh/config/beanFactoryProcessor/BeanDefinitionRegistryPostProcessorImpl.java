package cn.yjh.config.beanFactoryProcessor;

import cn.yjh.entity.Green;
import cn.yjh.entity.User;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.stereotype.Component;

/**
 * @description:
 * @author: You Jinhua
 * @create: 2021-02-26 15:37
 */
//@Component
public class BeanDefinitionRegistryPostProcessorImpl implements BeanDefinitionRegistryPostProcessor {
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		System.out.println("BeanDefinitionRegistry 后置处理器，执行 BeanDefinitionRegistryPostProcessor.postProcessBeanFactory()\n");
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		System.out.println("BeanDefinitionRegistry 后置处理器，执行 BeanDefinitionRegistryPostProcessor.postProcessBeanDefinitionRegistry()\n");
		//System.out.println("给容器中注册一个User类型的名字叫做 user1 的 BeanDefinition\n");
		//给容器中注册一个 BeanDefinition
//		registry.registerBeanDefinition("user1",new RootBeanDefinition(User.class));
		registry.registerBeanDefinition("green",new RootBeanDefinition(Green.class));

	}
}
