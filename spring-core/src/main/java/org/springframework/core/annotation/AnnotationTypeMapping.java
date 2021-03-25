/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import org.springframework.core.annotation.AnnotationTypeMapping.MirrorSets.MirrorSet;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Provides mapping information for a single annotation (or meta-annotation) in
 * the context of a root annotation type.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2
 * @see AnnotationTypeMappings
 */
final class AnnotationTypeMapping {


	private static final MirrorSet[] EMPTY_MIRROR_SETS = new MirrorSet[0];


	@Nullable
	// 假设此AnnotationTypeMapping实例为 MA, 映射的为@A_A  注解。
	private final AnnotationTypeMapping source;
	// 根注解。例如：解析 @Component 时，根注解是 @Service
	private final AnnotationTypeMapping root;
	// 当前注解距离 root 的距离。@Component 距离 @Service 的距离为 1
	private final int distance;
	// 当前注解 mapping 所对应的注解类型
	private final Class<? extends Annotation> annotationType;
	// 涉及到的注解类型列表。包括 source 的 metaTypes 加上 annotationType
	private final List<Class<? extends Annotation>> metaTypes;

	@Nullable
	// 当前注解类型的实例。
	private final Annotation annotation;
	// 当前注解的属性方法列表包装类
	private final AttributeMethods attributes;

	/**
	 * MirrorSet集合
	 *本注解里声明的属性，最终为同一个属性的 别名 的属性集合为一个MirrorSet
	 */
	private final MirrorSets mirrorSets;
	// 每个属性在root中对应的同名的属性方法的索引。与conventionMappings 的区别是，它是同名的属性，不考虑别名。
	private final int[] aliasMappings;
	// 方便访问属性 的映射消息，如果在root中有别名，则优先获取
	private final int[] conventionMappings;
	// 与annotationValueSource是相匹配的，定义每个属性最终从哪个注解的哪个属性获取值。
	private final int[] annotationValueMappings;

	private final AnnotationTypeMapping[] annotationValueSource;
	// 存储每个属性的所有别名属性方法（仅限于本注解定义中的属性方法），
	// Key：AliasFor里定义的属性方法，value为本注解内声明的属性方法。resolveAliasedForTargets方法中解析
	private final Map<Method, List<Method>> aliasedBy;
	// 本注解声明的所有属性方法的所有别名集合。最后用于注解定义检查然后会清空
	private final Set<Method> claimedAliases = new HashSet<>();


	AnnotationTypeMapping(@Nullable AnnotationTypeMapping source,
			Class<? extends Annotation> annotationType, @Nullable Annotation annotation) {

		this.source = source;
		this.root = (source != null ? source.getRoot() : this);
		this.distance = (source == null ? 0 : source.getDistance() + 1);
		this.annotationType = annotationType;
		this.metaTypes = merge(
				source != null ? source.getMetaTypes() : null,
				annotationType);
		this.annotation = annotation;
		this.attributes = AttributeMethods.forAnnotationType(annotationType);
		this.mirrorSets = new MirrorSets();
		this.aliasMappings = filledIntArray(this.attributes.size());
		this.conventionMappings = filledIntArray(this.attributes.size());
		this.annotationValueMappings = filledIntArray(this.attributes.size());
		this.annotationValueSource = new AnnotationTypeMapping[this.attributes.size()];
		//返回每个属性方法的所有别名（本注解声明的属性方法）。
		this.aliasedBy = resolveAliasedForTargets();
		processAliases();
		addConventionMappings();
		addConventionAnnotationValues();
	}


	private static <T> List<T> merge(@Nullable List<T> existing, T element) {
		if (existing == null) {
			return Collections.singletonList(element);
		}
		List<T> merged = new ArrayList<>(existing.size() + 1);
		merged.addAll(existing);
		merged.add(element);
		return Collections.unmodifiableList(merged);
	}

	/**
	 * 返回每个属性方法的所有别名（本注解声明的属性方法）。
	 * Key：AliasFor里定义的属性方法，value为本注解内声明的属性方法。
	 * @return
	 */
	private Map<Method, List<Method>> resolveAliasedForTargets() {
		Map<Method, List<Method>> aliasedBy = new HashMap<>();
		for (int i = 0; i < this.attributes.size(); i++) {
			// 声明的属性方法（作为value集合元素）
			Method attribute = this.attributes.get(i);
			AliasFor aliasFor = AnnotationsScanner.getDeclaredAnnotation(attribute, AliasFor.class);
			if (aliasFor != null) {
				// 最终解析的属性方法（作为key）
				Method target = resolveAliasTarget(attribute, aliasFor);
				aliasedBy.computeIfAbsent(target, key -> new ArrayList<>()).add(attribute);
			}
		}
		return Collections.unmodifiableMap(aliasedBy);
	}

	private Method resolveAliasTarget(Method attribute, AliasFor aliasFor) {
		return resolveAliasTarget(attribute, aliasFor, true);
	}

	//解析属性方法AliasFor的Method。
	private Method resolveAliasTarget(Method attribute, AliasFor aliasFor, boolean checkAliasPair) {
		if (StringUtils.hasText(aliasFor.value()) && StringUtils.hasText(aliasFor.attribute())) {
			throw new AnnotationConfigurationException(String.format(
					"In @AliasFor declared on %s, attribute 'attribute' and its alias 'value' " +
					"are present with values of '%s' and '%s', but only one is permitted.",
					AttributeMethods.describe(attribute), aliasFor.attribute(),
					aliasFor.value()));
		}
		Class<? extends Annotation> targetAnnotation = aliasFor.annotation();
		// 如果没有设置annotation属性，则表示是在本注解定义内。
		if (targetAnnotation == Annotation.class) {
			targetAnnotation = this.annotationType;
		}
		// aliased注解的属性。通过AliasFor的attribute或者value属性获取，
		String targetAttributeName = aliasFor.attribute();
		if (!StringUtils.hasLength(targetAttributeName)) {
			targetAttributeName = aliasFor.value();
		}
		// 如果attribute或者value都没有值，则获取aliased注解的属性方法名称
		if (!StringUtils.hasLength(targetAttributeName)) {
			targetAttributeName = attribute.getName();
		}
		//获取最终的aliased注解的属性方法
		Method target = AttributeMethods.forAnnotationType(targetAnnotation).get(targetAttributeName);
		if (target == null) {
			if (targetAnnotation == this.annotationType) {
				throw new AnnotationConfigurationException(String.format(
						"@AliasFor declaration on %s declares an alias for '%s' which is not present.",
						AttributeMethods.describe(attribute), targetAttributeName));
			}
			throw new AnnotationConfigurationException(String.format(
					"%s is declared as an @AliasFor nonexistent %s.",
					StringUtils.capitalize(AttributeMethods.describe(attribute)),
					AttributeMethods.describe(targetAnnotation, targetAttributeName)));
		}
		if (target.equals(attribute)) {
			throw new AnnotationConfigurationException(String.format(
					"@AliasFor declaration on %s points to itself. " +
					"Specify 'annotation' to point to a same-named attribute on a meta-annotation.",
					AttributeMethods.describe(attribute)));
		}
		if (!isCompatibleReturnType(attribute.getReturnType(), target.getReturnType())) {
			throw new AnnotationConfigurationException(String.format(
					"Misconfigured aliases: %s and %s must declare the same return type.",
					AttributeMethods.describe(attribute),
					AttributeMethods.describe(target)));
		}
		if (isAliasPair(target) && checkAliasPair) {
			AliasFor targetAliasFor = target.getAnnotation(AliasFor.class);
			if (targetAliasFor == null) {
				throw new AnnotationConfigurationException(String.format(
						"%s must be declared as an @AliasFor '%s'.",
						StringUtils.capitalize(AttributeMethods.describe(target)),
						attribute.getName()));
			}
			Method mirror = resolveAliasTarget(target, targetAliasFor, false);
			if (!mirror.equals(attribute)) {
				throw new AnnotationConfigurationException(String.format(
						"%s must be declared as an @AliasFor '%s', not '%s'.",
						StringUtils.capitalize(AttributeMethods.describe(target)),
						attribute.getName(), mirror.getName()));
			}
		}
		return target;
	}

	private boolean isAliasPair(Method target) {
		return target.getDeclaringClass().equals(this.annotationType);
	}

	private boolean isCompatibleReturnType(Class<?> attributeType, Class<?> targetType) {
		return Objects.equals(attributeType, targetType) ||
				Objects.equals(attributeType, targetType.getComponentType());
	}

	//处理别名，生成 MirrorSets，
	private void processAliases() {
		List<Method> aliases = new ArrayList<>();
		for (int i = 0; i < this.attributes.size(); i++) {
			aliases.clear();
			// 本属性方法的所有别名先加入。
			aliases.add(this.attributes.get(i));
			// 递归收集别名的别名
			collectAliases(aliases);
			if (aliases.size() > 1) {
				processAliases(i, aliases);
			}
		}
	}

	private void collectAliases(List<Method> aliases) {
		AnnotationTypeMapping mapping = this;
		while (mapping != null) {
			int size = aliases.size();
			for (int j = 0; j < size; j++) {
				List<Method> additional = mapping.aliasedBy.get(aliases.get(j));
				if (additional != null) {
					aliases.addAll(additional);
				}
			}
			mapping = mapping.source;
		}
	}

	/**
	 * 对每个属性方法，处理它的别名。
	 * aliases:每个属性方法的所有层级的别名。
	 */
	private void processAliases(int attributeIndex, List<Method> aliases) {
		//获取root声明的第一个别名属性的index。-1表示root不存在此属性方法的别名
		int rootAttributeIndex = getFirstRootAttributeIndex(aliases);
		AnnotationTypeMapping mapping = this;
		while (mapping != null) {
			//在root中有别名，并且此mapping不是root
			if (rootAttributeIndex != -1 && mapping != this.root) {
				for (int i = 0; i < mapping.attributes.size(); i++) {
					//如果别名中有此属性，则对应的属性index值为root的属性的index
					if (aliases.contains(mapping.attributes.get(i))) {
						mapping.aliasMappings[i] = rootAttributeIndex;
					}
				}
			}
			//更新mapping的mirrorSets
			mapping.mirrorSets.updateFrom(aliases);
			//mapping声明的属性方法的别名集合
			mapping.claimedAliases.addAll(aliases);
			if (mapping.annotation != null) {
				//返回本mapping每个属性最终取值的属性方法的序号 数组。
				int[] resolvedMirrors = mapping.mirrorSets.resolve(null,
						mapping.annotation, ReflectionUtils::invokeMethod);
				for (int i = 0; i < mapping.attributes.size(); i++) {
					//本属性方法是别名，则设置注解值的最终来源（mppaing和属性序号）
					if (aliases.contains(mapping.attributes.get(i))) {
						this.annotationValueMappings[attributeIndex] = resolvedMirrors[i];
						this.annotationValueSource[attributeIndex] = mapping;
					}
				}
			}
			mapping = mapping.source;
		}
	}

	private int getFirstRootAttributeIndex(Collection<Method> aliases) {
		AttributeMethods rootAttributes = this.root.getAttributes();
		for (int i = 0; i < rootAttributes.size(); i++) {
			if (aliases.contains(rootAttributes.get(i))) {
				return i;
			}
		}
		return -1;
	}

	//生成从root访问属性的方便属性方法信息
	private void addConventionMappings() {
		if (this.distance == 0) {
			return;
		}
		AttributeMethods rootAttributes = this.root.getAttributes();
		//此时，元素值全为-1.
		int[] mappings = this.conventionMappings;
		for (int i = 0; i < mappings.length; i++) {
			String name = this.attributes.get(i).getName();
			MirrorSet mirrors = getMirrorSets().getAssigned(i);
			// root中是否存在同名的属性
			int mapped = rootAttributes.indexOf(name);
			// root中存在同名的属性，并且属性名不为value
			if (!MergedAnnotation.VALUE.equals(name) && mapped != -1) {
				//存储root中的属性方法index。
				mappings[i] = mapped;
				if (mirrors != null) {
					for (int j = 0; j < mirrors.size(); j++) {
						// 同一属性的所有别名，设置成一样的root 属性index。
						mappings[mirrors.getAttributeIndex(j)] = mapped;
					}
				}
			}
		}
	}
	// 更新各级AnnotationTypeMapping的annotationValueMappings和annotationValueSource
	private void addConventionAnnotationValues() {
		for (int i = 0; i < this.attributes.size(); i++) {
			Method attribute = this.attributes.get(i);
			boolean isValueAttribute = MergedAnnotation.VALUE.equals(attribute.getName());
			AnnotationTypeMapping mapping = this;
			//在向root端（mapping.distance 比自己下的）遍历。
			while (mapping != null && mapping.distance > 0) {
				int mapped = mapping.getAttributes().indexOf(attribute.getName());
				//有同名属性
				if (mapped != -1  && isBetterConventionAnnotationValue(i, isValueAttribute, mapping)) {
					this.annotationValueMappings[i] = mapped;
					this.annotationValueSource[i] = mapping;
				}
				mapping = mapping.source;
			}
		}
	}
	//是更好的注解值获取属性方法（Value属性优先，distance较小的优先）
	private boolean isBetterConventionAnnotationValue(int index, boolean isValueAttribute,
			AnnotationTypeMapping mapping) {
		//原来没有获取值的属性方法
		if (this.annotationValueMappings[index] == -1) {
			return true;
		}
		int existingDistance = this.annotationValueSource[index].distance;
		return !isValueAttribute && existingDistance > mapping.distance;
	}

	/**
	 * Method called after all mappings have been set. At this point no further
	 * lookups from child mappings will occur.
	 */
	void afterAllMappingsSet() {
		validateAllAliasesClaimed();
		for (int i = 0; i < this.mirrorSets.size(); i++) {
			validateMirrorSet(this.mirrorSets.get(i));
		}
		this.claimedAliases.clear();
	}

	private void validateAllAliasesClaimed() {
		for (int i = 0; i < this.attributes.size(); i++) {
			Method attribute = this.attributes.get(i);
			AliasFor aliasFor = AnnotationsScanner.getDeclaredAnnotation(attribute, AliasFor.class);
			if (aliasFor != null && !this.claimedAliases.contains(attribute)) {
				Method target = resolveAliasTarget(attribute, aliasFor);
				throw new AnnotationConfigurationException(String.format(
						"@AliasFor declaration on %s declares an alias for %s which is not meta-present.",
						AttributeMethods.describe(attribute), AttributeMethods.describe(target)));
			}
		}
	}

	private void validateMirrorSet(MirrorSet mirrorSet) {
		Method firstAttribute = mirrorSet.get(0);
		Object firstDefaultValue = firstAttribute.getDefaultValue();
		for (int i = 1; i <= mirrorSet.size() - 1; i++) {
			Method mirrorAttribute = mirrorSet.get(i);
			Object mirrorDefaultValue = mirrorAttribute.getDefaultValue();
			if (firstDefaultValue == null || mirrorDefaultValue == null) {
				throw new AnnotationConfigurationException(String.format(
						"Misconfigured aliases: %s and %s must declare default values.",
						AttributeMethods.describe(firstAttribute), AttributeMethods.describe(mirrorAttribute)));
			}
			if (!ObjectUtils.nullSafeEquals(firstDefaultValue, mirrorDefaultValue)) {
				throw new AnnotationConfigurationException(String.format(
						"Misconfigured aliases: %s and %s must declare the same default value.",
						AttributeMethods.describe(firstAttribute), AttributeMethods.describe(mirrorAttribute)));
			}
		}
	}

	/**
	 * Get the root mapping.
	 * @return the root mapping
	 */
	AnnotationTypeMapping getRoot() {
		return this.root;
	}

	/**
	 * Get the source of the mapping or {@code null}.
	 * @return the source of the mapping
	 */
	@Nullable
	AnnotationTypeMapping getSource() {
		return this.source;
	}

	/**
	 * Get the distance of this mapping.
	 * @return the distance of the mapping
	 */
	int getDistance() {
		return this.distance;
	}

	/**
	 * Get the type of the mapped annotation.
	 * @return the annotation type
	 */
	Class<? extends Annotation> getAnnotationType() {
		return this.annotationType;
	}

	List<Class<? extends Annotation>> getMetaTypes() {
		return this.metaTypes;
	}

	/**
	 * Get the source annotation for this mapping. This will be the
	 * meta-annotation, or {@code null} if this is the root mapping.
	 * @return the source annotation of the mapping
	 */
	@Nullable
	Annotation getAnnotation() {
		return this.annotation;
	}

	/**
	 * Get the annotation attributes for the mapping annotation type.
	 * @return the attribute methods
	 */
	AttributeMethods getAttributes() {
		return this.attributes;
	}

	/**
	 * Get the related index of an alias mapped attribute, or {@code -1} if
	 * there is no mapping. The resulting value is the index of the attribute on
	 * the root annotation that can be invoked in order to obtain the actual
	 * value.
	 * @param attributeIndex the attribute index of the source attribute
	 * @return the mapped attribute index or {@code -1}
	 */
	int getAliasMapping(int attributeIndex) {
		return this.aliasMappings[attributeIndex];
	}

	/**
	 * Get the related index of a convention mapped attribute, or {@code -1}
	 * if there is no mapping. The resulting value is the index of the attribute
	 * on the root annotation that can be invoked in order to obtain the actual
	 * value.
	 * @param attributeIndex the attribute index of the source attribute
	 * @return the mapped attribute index or {@code -1}
	 */
	int getConventionMapping(int attributeIndex) {
		return this.conventionMappings[attributeIndex];
	}

	/**
	 * Get a mapped attribute value from the most suitable
	 * {@link #getAnnotation() meta-annotation}. The resulting value is obtained
	 * from the closest meta-annotation, taking into consideration both
	 * convention and alias based mapping rules. For root mappings, this method
	 * will always return {@code null}.
	 * @param attributeIndex the attribute index of the source attribute
	 * @return the mapped annotation value, or {@code null}
	 */
	@Nullable
	Object getMappedAnnotationValue(int attributeIndex) {
		int mapped = this.annotationValueMappings[attributeIndex];
		if (mapped == -1) {
			return null;
		}
		AnnotationTypeMapping source = this.annotationValueSource[attributeIndex];
		return ReflectionUtils.invokeMethod(source.attributes.get(mapped), source.annotation);
	}

	/**
	 * Determine if the specified value is equivalent to the default value of the
	 * attribute at the given index.
	 * @param attributeIndex the attribute index of the source attribute
	 * @param value the value to check
	 * @param valueExtractor the value extractor used to extract value from any
	 * nested annotations
	 * @return {@code true} if the value is equivalent to the default value
	 */
	boolean isEquivalentToDefaultValue(int attributeIndex, Object value,
			BiFunction<Method, Object, Object> valueExtractor) {

		Method attribute = this.attributes.get(attributeIndex);
		return isEquivalentToDefaultValue(attribute, value, valueExtractor);
	}

	/**
	 * Get the mirror sets for this type mapping.
	 * @return the mirrorSets the attribute mirror sets.
	 */
	MirrorSets getMirrorSets() {
		return this.mirrorSets;
	}


	private static int[] filledIntArray(int size) {
		int[] array = new int[size];
		Arrays.fill(array, -1);
		return array;
	}

	private static boolean isEquivalentToDefaultValue(Method attribute, Object value,
			BiFunction<Method, Object, Object> valueExtractor) {

		return areEquivalent(attribute.getDefaultValue(), value, valueExtractor);
	}

	private static boolean areEquivalent(@Nullable Object value, @Nullable Object extractedValue,
			BiFunction<Method, Object, Object> valueExtractor) {

		if (ObjectUtils.nullSafeEquals(value, extractedValue)) {
			return true;
		}
		if (value instanceof Class && extractedValue instanceof String) {
			return areEquivalent((Class<?>) value, (String) extractedValue);
		}
		if (value instanceof Class[] && extractedValue instanceof String[]) {
			return areEquivalent((Class[]) value, (String[]) extractedValue);
		}
		if (value instanceof Annotation) {
			return areEquivalent((Annotation) value, extractedValue, valueExtractor);
		}
		return false;
	}

	private static boolean areEquivalent(Class<?>[] value, String[] extractedValue) {
		if (value.length != extractedValue.length) {
			return false;
		}
		for (int i = 0; i < value.length; i++) {
			if (!areEquivalent(value[i], extractedValue[i])) {
				return false;
			}
		}
		return true;
	}

	private static boolean areEquivalent(Class<?> value, String extractedValue) {
		return value.getName().equals(extractedValue);
	}

	private static boolean areEquivalent(Annotation value, @Nullable Object extractedValue,
			BiFunction<Method, Object, Object> valueExtractor) {

		AttributeMethods attributes = AttributeMethods.forAnnotationType(value.annotationType());
		for (int i = 0; i < attributes.size(); i++) {
			Method attribute = attributes.get(i);
			if (!areEquivalent(ReflectionUtils.invokeMethod(attribute, value),
					valueExtractor.apply(attribute, extractedValue), valueExtractor)) {
				return false;
			}
		}
		return true;
	}


	/**
	 * A collection of {@link MirrorSet} instances that provides details of all
	 * defined mirrors.
	 */
	class MirrorSets {

		private MirrorSet[] mirrorSets;

		// 每个属性方法引用的 MirrorSet的 index。未引用 MirrorSet设置为 -1.
		private final MirrorSet[] assigned;

		MirrorSets() {
			// size为属性方法数量
			this.assigned = new MirrorSet[attributes.size()];
			this.mirrorSets = EMPTY_MIRROR_SETS;
		}

		/**
		 * 对每个mapping，此方法会调用很多次。解析的最终属性是同一个属性方法的，作为一个镜像组。
		 * aliases: 每个属性方法的所有层级的别名。
		 */
		void updateFrom(Collection<Method> aliases) {
			MirrorSet mirrorSet = null;
			//是别名的属性的个数
			int size = 0;
			//上一个别名属性的下标
			int last = -1;
			for (int i = 0; i < attributes.size(); i++) {
				Method attribute = attributes.get(i);
				//本注解定义的属性是其他属性的别名
				if (aliases.contains(attribute)) {
					size++;
					if (size > 1) {
						if (mirrorSet == null) {
							mirrorSet = new MirrorSet();
							this.assigned[last] = mirrorSet;
						}
						this.assigned[i] = mirrorSet;
					}
					last = i;
				}
			}
			if (mirrorSet != null) {
				mirrorSet.update();
				Set<MirrorSet> unique = new LinkedHashSet<>(Arrays.asList(this.assigned));
				unique.remove(null);
				this.mirrorSets = unique.toArray(EMPTY_MIRROR_SETS);
			}
		}

		int size() {
			return this.mirrorSets.length;
		}

		MirrorSet get(int index) {
			return this.mirrorSets[index];
		}

		@Nullable
		MirrorSet getAssigned(int attributeIndex) {
			return this.assigned[attributeIndex];
		}

		/**
		 * 返回本mapping每个属性最终取值的属性方法的序号 数组。
		 * source:注解
		 * annotation：注解实例
		 * valueExtractor：
		 * @return
		 */
		int[] resolve(@Nullable Object source, @Nullable Object annotation,
				BiFunction<Method, Object, Object> valueExtractor) {

			int[] result = new int[attributes.size()];
			for (int i = 0; i < result.length; i++) {
				result[i] = i;
			}
			for (int i = 0; i < size(); i++) {
				MirrorSet mirrorSet = get(i);
				//返回 mirrorSet中第一个不是默认值的属性的序号。
				int resolved = mirrorSet.resolve(source, annotation, valueExtractor);
				//设置属性方法的最终取值的属性方法的序号
				for (int j = 0; j < mirrorSet.size; j++) {
					result[mirrorSet.indexes[j]] = resolved;
				}
			}
			return result;
		}


		/**
		 * A single set of mirror attributes.
		 */
		class MirrorSet {

			//此 MirrorSet被引用了多少次
			private int size;

			/**
			 * 表示 MirrorSet每次被引用的属性的序号。
			 * 注解镜像属性方法索引数组，size为属性方法数量。数组下标代表找到的第n-1个镜像方法，值为镜像方法的索引。
			 * 例如方法3或者4互相镜像，则0:3,1:4，后续元素值都为-1.
			 */
			private final int[] indexes = new int[attributes.size()];

			//更新状态，根据 MirrorSets.assigned
			void update() {
				this.size = 0;
				Arrays.fill(this.indexes, -1);
				for (int i = 0; i < MirrorSets.this.assigned.length; i++) {
					if (MirrorSets.this.assigned[i] == this) {
						this.indexes[this.size] = i;
						this.size++;
					}
				}
			}

			//返回第一个不是默认值的方法的索引。
			<A> int resolve(@Nullable Object source, @Nullable A annotation,
					BiFunction<Method, Object, Object> valueExtractor) {

				int result = -1;
				Object lastValue = null;
				for (int i = 0; i < this.size; i++) {
					Method attribute = attributes.get(this.indexes[i]);
					//获取到属性的值
					Object value = valueExtractor.apply(attribute, annotation);
					boolean isDefaultValue = (value == null ||
							isEquivalentToDefaultValue(attribute, value, valueExtractor));
					//如果属性值不是默认值，并且与上一个值不相等，则继续判断
					if (isDefaultValue || ObjectUtils.nullSafeEquals(lastValue, value)) {
						continue;
					}
					//上一个值不为 null 并且 与上一个值不相等，则抛出异常。
					if (lastValue != null &&
							!ObjectUtils.nullSafeEquals(lastValue, value)) {
						String on = (source != null) ? " declared on " + source : "";
						throw new AnnotationConfigurationException(String.format(
								"Different @AliasFor mirror values for annotation [%s]%s; attribute '%s' " +
								"and its alias '%s' are declared with values of [%s] and [%s].",
								getAnnotationType().getName(), on,
								attributes.get(result).getName(),
								attribute.getName(),
								ObjectUtils.nullSafeToString(lastValue),
								ObjectUtils.nullSafeToString(value)));
					}
					//更新 result，并更新 lastValue
					result = this.indexes[i];
					lastValue = value;
				}
				return result;
			}

			int size() {
				return this.size;
			}

			Method get(int index) {
				int attributeIndex = this.indexes[index];
				return attributes.get(attributeIndex);
			}

			int getAttributeIndex(int index) {
				return this.indexes[index];
			}
		}
	}

}
