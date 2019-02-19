package com.github.mybatis.helper.core;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 执行sql工具类
 * @author will
 */
public class ExecuteHelper {
    private Connection connection;
    private MappedStatement mappedStatement;
    private BoundSql boundSql;

    private ExecuteHelper(Connection connection,MappedStatement mappedStatement,BoundSql boundSql){
        this.connection=connection;
        this.mappedStatement=mappedStatement;
        this.boundSql=boundSql;
    }

    public static ExecuteHelper build(Connection connection,MappedStatement mappedStatement,BoundSql boundSql){
        return new ExecuteHelper(connection,mappedStatement,boundSql);
    }

    /**
     * 执行查询
     * @param action 执行回调
     * @param sql
     * @param isUseParameterHandler
     * @param <T>
     * @return
     */
    public <T> T exeQuery(ExecuteHelper.Action action, String sql,boolean isUseParameterHandler) {
        BoundSql newBoundSql = new BoundSql(mappedStatement.getConfiguration(), sql, boundSql.getParameterMappings(), boundSql.getParameterObject());
        for(ParameterMapping mapping:boundSql.getParameterMappings()){
            String prop=mapping.getProperty();
            if (boundSql.hasAdditionalParameter(prop)) {
                newBoundSql.setAdditionalParameter(prop,boundSql.getAdditionalParameter(prop));
            }
        }
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;
        try {
            pstmt = connection.prepareStatement(sql);
            if(isUseParameterHandler){
                new DefaultParameterHandler(mappedStatement, boundSql.getParameterObject(), newBoundSql).setParameters(pstmt);
            }
            resultSet = pstmt.executeQuery();
            return action.doAction(resultSet);
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            if(resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if(pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 执行更新
     * @param sql
     * @param isUseParameterHandler
     * @return 成功的条数
     */
    public int exeUpdate(String sql,boolean isUseParameterHandler) {
        BoundSql totalBoundSql = new BoundSql(mappedStatement.getConfiguration(), sql, boundSql.getParameterMappings(), boundSql.getParameterObject());
        ParameterHandler parameterHandler = new DefaultParameterHandler(mappedStatement, boundSql.getParameterObject(), totalBoundSql);
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;
        try {
            pstmt = connection.prepareStatement(sql);
            if(isUseParameterHandler){
                new DefaultParameterHandler(mappedStatement, boundSql.getParameterObject(), totalBoundSql).setParameters(pstmt);
            }
            return pstmt.executeUpdate(sql);
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            if(resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if(pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }

    /**
     * 执行sql回调
     */
    public interface Action{
        <T> T doAction(ResultSet resultSet)throws SQLException;
    }
}
