import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @description:
 * @author: You Jinhua
 * @create: 2021-03-16 11:27
 */
@AnnotationUtilsTest.Root(value = "com.yjh",basePackages = "base.yjh",a="aa",b="bb")
public class AnnotationUtilsTest {

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE})
	@interface  Level2 {
		@AliasFor("basePackages")
		String[] value() default {};

		@AliasFor("value")
		String[] basePackages() default {};
	}



	@Level2
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE})
	@interface  Level1 {
		@AliasFor(value = "basePackages",annotation = Level2.class)
		String[] value() default {};

		//@AliasFor(value = "value",annotation = Level2.class)
		String[] basePackages() default {};
	}

	@Level1
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE})
	@interface  Root {

		@AliasFor(annotation = Level1.class)
		String[] value() default {};

		@AliasFor(annotation = Level1.class)
		String[] basePackages() default {};

		@AliasFor("b")
		String a() default "";

		@AliasFor("c")
		String b() default "";

		@AliasFor("a")
		String c() default "";

	}


	public static void main(String[] args) {
		AnnotationAttributes root = AnnotatedElementUtils.findMergedAnnotationAttributes(AnnotationUtilsTest.class,Root.class,false,true);
		System.out.println("Root = "+root);
		/*AnnotationAttributes leve1 = AnnotatedElementUtils.findMergedAnnotationAttributes(AnnotationUtilsTest.class,Level1.class,false,true);
		Level1 mergedAnnotation = AnnotatedElementUtils.getMergedAnnotation(AnnotationUtilsTest.class, Level1.class);
		System.out.println("Level1 = "+leve1);*/
		/*AnnotationAttributes leve2 = AnnotatedElementUtils.findMergedAnnotationAttributes(AnnotationUtilsTest.class,Level2.class,false,true);
		System.out.println("Level2 = "+leve2);*/

	}


}
