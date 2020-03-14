package com.github.mybatis.helper.datascope.annotation;

/**
 * sql风格
 * inner是在where后面直接添加条件，outer是在外面包一个查询语句
 */
public enum DataScopeSqlStyle {
    INNER,OUTER
}
