package com.itheima.util;

/**
 * ThreadLocal 工具类
 */
@SuppressWarnings("all")
//多线程下的线程安全,先在登录拦截器logininterceptor中获取token,再在拦截器中获取用户信息,
public class ThreadLocalUtil {
    //提供ThreadLocal对象,
    private static final ThreadLocal THREAD_LOCAL = new ThreadLocal();

    //根据键获取值
    public static <T> T get(){
        return (T) THREAD_LOCAL.get();
    }
	
    //存储键值对
    public static void set(Object value){
        THREAD_LOCAL.set(value);
    }


    //清除ThreadLocal 防止内存泄漏
    public static void remove(){
        THREAD_LOCAL.remove();
    }
}
