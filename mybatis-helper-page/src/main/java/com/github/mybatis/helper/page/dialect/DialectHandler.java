package com.github.mybatis.helper.page.dialect;

import com.github.mybatis.helper.page.dialect.helper.Dialect;
import com.github.mybatis.helper.page.dialect.helper.MySqlDialect;
import com.github.mybatis.helper.page.dialect.helper.OracleDialect;
import com.github.mybatis.helper.page.dialect.helper.PostgreSqlDialect;
import org.apache.ibatis.mapping.MappedStatement;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
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

    public static Dialect getDialect(MappedStatement mappedStatement){
        DataSource dataSource = mappedStatement.getConfiguration().getEnvironment().getDataSource();
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            String jdbcUrl=conn.getMetaData().getURL();
            for (String dialect : dialectMap.keySet()) {
                if (jdbcUrl.indexOf(":" + dialect + ":") != -1) {
                    return dialectMap.get(dialect);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    //ignore
                }
            }
        }
        return null;
    }
}
