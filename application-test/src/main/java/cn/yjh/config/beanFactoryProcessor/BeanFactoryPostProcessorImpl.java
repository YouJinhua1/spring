package cn.yjh.config.beanFactoryProcessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * @description:
 * @author: You Jinhua
 * @create: 2021-02-26 15:35
 */
//@Component
public class BeanFactoryPostProcessorImpl implements BeanFactoryPostProcessor {
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		System.out.println("BeanFactory 后置处理器，执行 BeanFactoryPostProcessor.postProcessBeanFactory()\n");
	}
}
