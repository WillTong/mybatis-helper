package com.github.mybatis.helper.page.dialect;

import com.github.mybatis.helper.page.dialect.helper.Dialect;
import com.github.mybatis.helper.page.dialect.helper.MySqlDialect;
import com.github.mybatis.helper.page.dialect.helper.OracleDialect;
import com.github.mybatis.helper.page.dialect.helper.PostgreSqlDialect;

import java.util.HashMap;
import java.util.Map;

/**
 * 断言判断.
 * @author will
 */
public class DialectHandler {
    private static Map<String,Dialect> dialectMap=new HashMap<>();

    static{
        dialectMap.put("mysql", new MySqlDialect());
        dialectMap.put("oracle", new OracleDialect());
        dialectMap.put("postgresql", new PostgreSqlDialect());
    }

    public static Dialect getDialect(String dbType){
        return dialectMap.get(dbType);
    }
}
