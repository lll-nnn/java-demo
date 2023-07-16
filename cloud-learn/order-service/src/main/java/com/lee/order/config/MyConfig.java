package com.lee.order.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class MyConfig {
    @Bean
    @LoadBalanced   //负载均衡  默认规则为轮询
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

//    @Bean
//    public IRule randomRule(){  //配置负载均衡方案  随机    对全局均衡
//        return new RandomRule();
//    }
}
