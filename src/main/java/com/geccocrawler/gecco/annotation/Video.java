package com.geccocrawler.gecco.annotation;

import java.lang.annotation.*;

/**
 * 表示该字段是一个视频类型的元素，jsoup会默认获取元素的source属性值。属性必须是String类型。
 * 
 * @author huchengyi
 *
 */
@Inherited
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Video {

	/**
	 * 默认获取src属性值，可以多选，按顺序查找
	 * 
	 * @return 属性名称
	 */
	String[] value() default "src";
	
	/**
	 * 如果填写本地路径将会自动下载到本地
	 * 
	 * @return 本地路径
	 */
	String download() default "";
}
