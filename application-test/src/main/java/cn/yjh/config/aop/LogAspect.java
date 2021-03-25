package cn.yjh.config.aop;

import cn.yjh.config.aop.annotation.ControllerLog;
import cn.yjh.config.aop.annotation.ServiceLog;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @description:
 * @author: You Jinhua
 * @create: 2021-02-25 14:42
 */
//@Aspect
//@Component
public class LogAspect {

	/*Controller层切点*/
	@Pointcut("@annotation(cn.yjh.config.aop.annotation.ControllerLog)")
	public void controllerAspect() {
	}


	/**
	 * 前置通知
	 *
	 * @param joinPoint
	 */
	@Before("controllerAspect()")
	public void doBefore(JoinPoint joinPoint) throws Exception {
		String user_name = "unKnow";
		/*String ip = "unKnow";
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		Map<String, String[]> parameterMap = request.getParameterMap();
		if (request != null && request.getSession() != null) {
			user_name = request.getParameter("username");
		}

		Object[] args = null;
		Object[] arguments = null;
		String IP="127.0.0.1";

		if (joinPoint != null) {
			args = joinPoint.getArgs();
		}
		if (args != null) {
			arguments = new Object[args.length];
		}

		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof ServletRequest || args[i] instanceof ServletResponse || args[i] instanceof MultipartFile) {
				continue;
			}
			arguments[i] = args[i];
		}
		StringBuffer sb = new StringBuffer();
		for(Object arg:arguments){
			sb.append(arg.toString()+"  ");
		}
*/

		System.out.println("【请求方法：" + (joinPoint.getTarget().getClass().getName() + "." + joinPoint.getSignature().getName()) + "  方法描述：" + getControllerMethodDescription(joinPoint) + "】");
	}

	/**
	 * 后置通知
	 *
	 * @param joinPoint
	 */
	@After("controllerAspect()")
	public void doAfter(JoinPoint joinPoint) throws Exception {
		System.out.println("【请求方法：" + (joinPoint.getTarget().getClass().getName() + "." + joinPoint.getSignature().getName()) + "  方法描述：" + getControllerMethodDescription(joinPoint) + "】");
	}

	/**
	 * 获取注解中对方法的描述信息 用于Controller层注解
	 *
	 * @param joinPoint
	 * @return
	 * @throws Exception
	 */
	public static String getControllerMethodDescription(JoinPoint joinPoint) throws Exception {
		String targetName = joinPoint.getTarget().getClass().getName();
		String methodName = joinPoint.getSignature().getName();//目标方法名
		Object[] arguments = joinPoint.getArgs();
		Class<?> targetClass = Class.forName(targetName);
		Method[] methods = targetClass.getMethods();
		String description = "";
		for (Method method : methods) {
			if (method.getName().equals(methodName)) {
				Class<?>[] clazzs = method.getParameterTypes();
				if (clazzs.length == arguments.length) {
					description = method.getAnnotation(ControllerLog.class).description();
					break;
				}
			}
		}
		return description;
	}


}
