package cn.yjh.config.anno_import;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;


/**
 * @description: 实现 ImportSelector 接口给容器中添加组件
 * @author: You Jinhua
 * @create: 2021-02-28 13:52
 */
public class ImportSelectorImpl implements ImportSelector {

	// 返回值就是要导入的组件的全类名的数组
	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		String [] classes = new String[1];
		classes[0]="cn.yjh.entity.Dog";
		return classes;
	}
}
