package com.github.mybatis.helper.datascope.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface ActiveScopeField{

    /**
     * dataScope的Map的key
     * @return
     */
    String value();

    /**
     * 如果列名和dataScope不一致通过columnName指定新的列名
     * @return
     */
    String columnName() default "";
}
