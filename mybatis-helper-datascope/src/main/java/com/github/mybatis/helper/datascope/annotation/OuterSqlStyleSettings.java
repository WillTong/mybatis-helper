package com.github.mybatis.helper.datascope.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface OuterSqlStyleSettings {

    /**
     * 自定义外层select语句
     * @return
     */
    String select() default "T.*";
}
