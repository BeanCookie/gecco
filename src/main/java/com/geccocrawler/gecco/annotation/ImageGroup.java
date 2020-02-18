package com.geccocrawler.gecco.annotation;

import java.lang.annotation.*;

/**
 * 表示该字段是一个图片组的父文件夹。属性必须是String类型。
 * 
 * @author huchengyi
 *
 */
@Inherited
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ImageGroup {
}
