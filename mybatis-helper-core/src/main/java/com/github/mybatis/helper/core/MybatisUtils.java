package com.github.mybatis.helper.core;

import java.lang.reflect.Method;

/**
 * mybatis工具.
 * @author will
 */
public class MybatisUtils {

    /**
     * 获取类名
     * @param mappedStatementId
     * @return
     */
    public static String getClassName(String mappedStatementId){
        return mappedStatementId.substring(0,mappedStatementId.lastIndexOf("."));
    }

    /**
     * 获取反射类
     * @param mappedStatementId
     * @return
     * @throws ClassNotFoundException
     */
    public static Class getClass(String mappedStatementId) throws ClassNotFoundException {
        return Class.forName(MybatisUtils.getClassName(mappedStatementId));
    }

    /**
     * 获取方法名
     * @param mappedStatementId
     * @return
     */
    public static String getMethodName(String mappedStatementId){
        return mappedStatementId.substring(mappedStatementId.lastIndexOf(".")+1);
    }

    /**
     * 获取反射方法
     * @param mappedStatementId
     * @param clazz
     * @return
     * @throws NoSuchMethodException
     */
    public static Method getMethod(String mappedStatementId,Class clazz) throws NoSuchMethodException {
        for(Method method:clazz.getMethods()){
            if(method.getName().equals(MybatisUtils.getMethodName(mappedStatementId))){
                return method;
            }
        }
        throw new NoSuchMethodException();
    }

    /**
     * 获取反射方法
     * @param mappedStatementId
     * @return
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     */
    public static Method getMethod(String mappedStatementId) throws NoSuchMethodException,ClassNotFoundException {
        Class clazz=Class.forName(MybatisUtils.getClassName(mappedStatementId));
        for(Method method:clazz.getMethods()){
            if(method.getName().equals(MybatisUtils.getMethodName(mappedStatementId))){
                return method;
            }
        }
        throw new NoSuchMethodException();
    }

    /**
     * 判断MappedStatementId是否在执行之列
     * @param mappedStatementId
     * @param mappedStatementIdMatches
     * @return
     */
    public static int matchMappedStatementId(String mappedStatementId,String[] mappedStatementIdMatches){
        if(mappedStatementIdMatches==null){
            return -1;
        }
        for(String mappedStatementIdMatch:mappedStatementIdMatches){
            if(mappedStatementIdMatch.equals(mappedStatementId)){
                return mappedStatementId.length();
            }
            if("*".equals(mappedStatementIdMatch)){
                return 0;
            }
            if(mappedStatementId.startsWith(mappedStatementIdMatch)){
                return mappedStatementIdMatch.length();
            }
        }
        return -1;
    }
}
