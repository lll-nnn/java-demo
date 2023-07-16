package com.lee.user.controller;

import com.lee.user.config.PatternProperties;
import com.lee.user.pojo.User;
import com.lee.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/user")
//@RefreshScope   //将nacos中的属性刷新
public class UserController {

    @Autowired
    private UserService userService;

//    @Value("${pattern.dateformat}")
//    private String dateformat;

    @Autowired
    private PatternProperties patternProperties;

    @GetMapping("/now")
    public String getNow(){
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(patternProperties.getDateformat()));
    }

    @GetMapping("/prop")
    public PatternProperties getProp(){
        return patternProperties;
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable("id")Long id,
                        @RequestHeader(value = "hello", required = false)String hello){
        System.out.println(hello);
        return userService.queryById(id);
    }
}
