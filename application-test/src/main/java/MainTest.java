import cn.yjh.config.AppConfig;
import cn.yjh.controller.UserController;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;



/**
 * @description:
 * @author: You Jinhua
 * @create: 2021-02-21 06:22
 */
public class MainTest {


	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
		String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
		for (String beanName : beanDefinitionNames){
			System.out.println(beanName);
		}
		UserController userController = applicationContext.getBean(UserController.class);
		userController.query();
	}


}
