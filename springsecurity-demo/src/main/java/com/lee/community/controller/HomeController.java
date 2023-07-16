package com.lee.community.controller;

import com.lee.community.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "redirect:/index";
    }

    @RequestMapping(path = "/index", method = RequestMethod.GET)
    public String getIndexPage(Model model) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User){
            model.addAttribute("loginUser", principal);
        }
        return "/index";
    }

    @RequestMapping(path = "/discuss", method = RequestMethod.GET)
    public String getDiscussPage() {
        return "/site/discuss";
    }

    @RequestMapping(path = "/letter", method = RequestMethod.GET)
    public String getLetterPage() {
        return "/site/letter";
    }

    @RequestMapping(path = "/admin", method = RequestMethod.GET)
    public String getAdminPage() {
        return "/site/admin";
    }

    @RequestMapping(path = "/loginpage", method = {RequestMethod.GET, RequestMethod.POST})
    public String getLoginPage() {
        return "/site/login";
    }

    @GetMapping("/denied")
    public String getDeniedPage(){
        return "/error/404";
    }
}
