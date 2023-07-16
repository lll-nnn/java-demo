package com.lee.order.controller;

import com.lee.order.pojo.Order;
import com.lee.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/order/{id}")
    public Order getOrder(@PathVariable("id")Long id,
                          @RequestHeader(value = "hello", required = false) String hello){
        System.out.println(hello);
        return orderService.queryOrderById(id);
    }
}
