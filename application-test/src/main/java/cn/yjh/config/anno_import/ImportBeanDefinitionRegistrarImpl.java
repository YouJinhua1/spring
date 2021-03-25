package cn.yjh.config.anno_import;

import cn.yjh.entity.Pig;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @description: 实现 ImportBeanDefinitionRegistrar 接口给容器中添加组件
 * @author: You Jinhua
 * @create: 2021-02-28 14:18
 */
public class ImportBeanDefinitionRegistrarImpl implements ImportBeanDefinitionRegistrar {
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry, BeanNameGenerator importBeanNameGenerator) {
		// 给容器中注册一个 pig 对象
		registry.registerBeanDefinition("pig",new RootBeanDefinition(Pig.class));
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

	}
}
