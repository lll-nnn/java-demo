package com.lee.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lee.dto.LoginFormDTO;
import com.lee.dto.Result;
import com.lee.entity.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result logout(HttpServletRequest request);

    Result getUserById(Long id);

    Result sign();

    Result signCount();
}
