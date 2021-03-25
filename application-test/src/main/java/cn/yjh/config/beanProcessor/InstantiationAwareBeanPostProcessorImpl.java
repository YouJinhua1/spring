package cn.yjh.config.beanProcessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * @description:
 * @author: You Jinhua
 * @create: 2021-02-25 09:04
 */
@Component
public class InstantiationAwareBeanPostProcessorImpl implements InstantiationAwareBeanPostProcessor {

	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		System.out.println(beanName +"  实例化 【 前 】，执行 InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation()\n");
		return null;
	}

	@Override
	public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		System.out.println(beanName +"  实例化 【 后 】，初始化 【 前 】，执行 InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation()\n");
		return true;
	}

	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
		System.out.println("自定义=======InstantiationAwareBeanPostProcessorImpl=======postProcessProperties");
		return pvs;
	}


}
