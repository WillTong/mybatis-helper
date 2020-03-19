package com.github.mybatis.helper.dolog.dialect;

import com.github.mybatis.helper.dolog.dialect.helper.Dialect;
import com.github.mybatis.helper.dolog.dialect.helper.MySqlDialect;
import com.github.mybatis.helper.dolog.dialect.helper.OracleDialect;

import java.util.HashMap;
import java.util.Map;

/**
 * 断言判断.
 * @author will
 */
public class DialectHandler {
    private static Map<String, Dialect> dialectMap=new HashMap<>();

    static{
        dialectMap.put("mysql", new MySqlDialect());
        dialectMap.put("oracle", new OracleDialect());
    }

    public static Dialect getDialect(String dbType){
        return dialectMap.get(dbType);
    }
}
