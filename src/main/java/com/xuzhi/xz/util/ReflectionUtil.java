package com.xuzhi.xz.util;

public class ReflectionUtil {
    /**
     * 创建实例
     */
    public static Object newInstance(Class clazz){
        Object instance = null;
        try {
            instance = clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return instance;
    }


    public static Object newInstance(String className){
        Class clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return newInstance(clazz);
    }
}
