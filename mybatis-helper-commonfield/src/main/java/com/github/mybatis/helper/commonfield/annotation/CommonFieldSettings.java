package com.github.mybatis.helper.commonfield.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface CommonFieldSettings {
    /**
     * 新增操作人存储字段
     * @return
     */
    String insertPersionColumn() default "CREATED_BY";
    /**
     * 新增操作日期存储字段
     * @return
     */
    String insertDateColumn() default "CREATED_DATE";
    /**
     * 修改操作人存储字段
     * @return
     */
    String updatePersionColumn() default "LAST_UPD_BY";
    /**
     * 修改操作日期存储字段
     * @return
     */
    String updateDateColumn() default "LAST_UPD_DATE";
}
