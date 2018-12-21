package com.xuzhi.xz.aop.proxy;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public abstract class AbstractProxyFactory implements MethodInterceptor {
    /**
     * 要代理的对象
     * */
    private Object target;

    public String getProxyMethodName() {
        return proxyMethodName;
    }

    public void setProxyMethodName(String proxyMethodName) {
        this.proxyMethodName = proxyMethodName;
    }

    /**
     * 要增强的方法
     * */
    private String proxyMethodName;

    public Object createProxy(Object target){
        this.target = target;
        //enhancer类用于生成代理对象
        Enhancer enhancer = new Enhancer();

        enhancer.setSuperclass(this.target.getClass());
        enhancer.setCallback(this);
        return enhancer.create();
    }


    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        Object result;

        String proxyMethod = getProxyMethodName();
        if (proxyMethod.equals(method.getName())){
            doBefore();
        }

        result = methodProxy.invokeSuper(o, objects);

        if (proxyMethod.equals(method.getName())){
            doAfter();
        }

        return result;
    }

    public abstract void doBefore();

    public abstract void doAfter();
}
