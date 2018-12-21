package com.xuzhi.xz.service.impl;

import com.xuzhi.xz.annotation.XzService;
import com.xuzhi.xz.service.OrderService;

@XzService("OrderServiceImpl")
public class OrderServiceImpl implements OrderService {
    public String query(String name, String age) {
        return "name="+name+"  ,age="+age;
    }
}
