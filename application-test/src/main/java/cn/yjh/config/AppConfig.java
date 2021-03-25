package cn.yjh.config;

import cn.yjh.config.anno_import.ImportBeanDefinitionRegistrarImpl;
import cn.yjh.config.anno_import.ImportSelectorImpl;
import cn.yjh.entity.User;
import org.springframework.context.annotation.*;

/**
 * @description:
 * @author: You Jinhua
 * @create: 2021-02-21 06:24
 */
@Configuration
//@EnableAspectJAutoProxy
@ComponentScan("cn.yjh")
//@Import({ImportSelectorImpl.class, ImportBeanDefinitionRegistrarImpl.class})
public class AppConfig {

	/*@Scope("prototype")
	@Bean
	public User user(){
		return new User();
	}*/
}
