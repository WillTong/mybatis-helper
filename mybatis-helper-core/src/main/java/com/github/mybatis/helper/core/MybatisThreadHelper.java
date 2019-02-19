package com.github.mybatis.helper.core;

import java.util.HashMap;
import java.util.Map;

/**
 * 线程安全变量存储
 * @author will
 */
public class MybatisThreadHelper {

    private static ThreadLocal<Map<String,Object>> mybatisThreadVariable = new InheritableThreadLocal();

    public static void putVariable(String key,Object value) {
        if(mybatisThreadVariable.get()==null){
            mybatisThreadVariable.set(new HashMap<>());
        }
        mybatisThreadVariable.get().put(key,value);
    }

    public static boolean containsVariableKey(String key) {
        if(mybatisThreadVariable.get()!=null){
            return mybatisThreadVariable.get().containsKey(key);
        }else{
            return false;
        }
    }

    public static Object getVariable(String key) {
        if(mybatisThreadVariable.get()!=null){
            return mybatisThreadVariable.get().get(key);
        }else{
            return null;
        }
    }

    public static void clearVariable() {
        try{
            mybatisThreadVariable.remove();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
