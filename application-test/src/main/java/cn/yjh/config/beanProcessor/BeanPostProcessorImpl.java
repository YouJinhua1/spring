package cn.yjh.config.beanProcessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * @description:
 * @author: You Jinhua
 * @create: 2021-02-26 15:35
 */
@Component
public class BeanPostProcessorImpl implements BeanPostProcessor {

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		System.out.println(beanName +"  实例化 【 后 】，初始化 【 前 】，执行 BeanPostProcessor.postProcessBeforeInitialization()\n");
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		System.out.println(beanName +"  实例化 【 后 】，初始化 【 后 】，执行 BeanPostProcessor.postProcessAfterInitialization()\n");
		return bean;
	}
}
