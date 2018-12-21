package com.xuzhi.xz.controller;

import com.xuzhi.xz.annotation.XzAutowired;
import com.xuzhi.xz.annotation.XzController;
import com.xuzhi.xz.annotation.XzRequestMapping;
import com.xuzhi.xz.annotation.XzRequestParam;
import com.xuzhi.xz.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@XzController
@XzRequestMapping("/xzzz")
public class OrderController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @XzAutowired("OrderServiceImpl")
    private OrderService orderService;

    public OrderService getOrderService() {
        return orderService;
    }

    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    @XzRequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response,
                      @XzRequestParam("name") String name,
                      @XzRequestParam("age") String age){
        try {
            PrintWriter pw = response.getWriter();
            String result = orderService.query(name,age);
            logger.info("name="+name+",age="+age);
            pw.write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
