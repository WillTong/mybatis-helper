package com.github.mybatis.helper.datascope.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,ElementType.METHOD})
public @interface DataScopeFilterSettings {
    /**
     * 自定义dataScope生效的字段
     * @return
     */
    ActiveScopeField[] activeScopeFields() default {};

    /**
     * 自定义外层select语句
     * @return
     */
    String select() default "T.*";
}
