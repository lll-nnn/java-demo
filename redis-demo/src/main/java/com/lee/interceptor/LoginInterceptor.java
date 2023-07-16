package com.lee.interceptor;

import com.lee.dto.UserDTO;
import com.lee.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        HttpSession session = request.getSession();
//        UserDTO user =(UserDTO) session.getAttribute("user");
//        if (user == null){
//            response.setStatus(401);
//            return false;
//        }
//        UserHolder.saveUser(user);

        UserDTO userDTO = UserHolder.getUser();
        if (userDTO == null){
            response.setStatus(401);
            return false;
        }
        return true;
    }
}
