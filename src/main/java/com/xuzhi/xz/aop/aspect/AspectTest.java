package com.xuzhi.xz.aop.aspect;

import com.xuzhi.xz.aop.annotation.XzAspect;
import com.xuzhi.xz.aop.annotation.XzPointcut;
import com.xuzhi.xz.aop.proxy.AbstractProxyFactory;

@XzAspect
public class AspectTest extends AbstractProxyFactory {

    @XzPointcut("com.xuzhi.xz.service.impl.OrderServiceImpl:query()")
    public void testAspect() {
    }

    @Override
    public void doBefore() {
        System.out.println("before query");
    }

    @Override
    public void doAfter() {
        System.out.println("after query");
    }
}
