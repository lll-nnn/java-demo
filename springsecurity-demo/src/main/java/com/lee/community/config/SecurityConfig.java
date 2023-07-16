package com.lee.community.config;

import com.lee.community.entity.User;
import com.lee.community.service.UserService;
import com.lee.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.InMemoryTokenRepositoryImpl;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserService userService;

    /**
     * 忽略静态文件
     * @param web
     * @throws Exception
     */
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**");
    }

    //认证
    //AuthenticationManager:认证核心接口
    //AuthenticationManagerBuilder:构建AuthenticationManager
    //ProviderManager:AuthenticationManager接口的默认实现类
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        //内置认证规则
//        auth.userDetailsService(userService).passwordEncoder(new Pbkdf2PasswordEncoder("123456"));
        //自定义认证规则
        //AuthenticationProvider:ProviderManager持有一组AuthenticationProvider，
        //                      每个AuthenticationProvider负责一种认证
        //委托模式：ProviderManager将认证委托给AuthenticationProvider
        auth.authenticationProvider(new AuthenticationProvider() {
            //Authentication：封装认证信息的接口，不同的实现类代表不同的认证信息
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                String username = authentication.getName();
                String password = (String) authentication.getCredentials();
                User user = userService.findUserByName(username);
                if (user == null){
                    throw new UsernameNotFoundException("账号不存在！");
                }
                password = CommunityUtil.md5(password + user.getSalt());
                if (!user.getPassword().equals(password)){
                    throw new BadCredentialsException("密码错误！");
                }
                //principal：认证的主要信息 credentials：凭证  credentials：认证的权限
                return new UsernamePasswordAuthenticationToken(
                        user, user.getPassword(), user.getAuthorities());
            }

            //支持的认证类型
            @Override
            public boolean supports(Class<?> aClass) {
                //UsernamePasswordAuthenticationToken账号密码认证
                return UsernamePasswordAuthenticationToken.class.equals(aClass);
            }
        });
    }

    //授权
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //登录相关配置
        http.formLogin()
                .loginPage("/loginpage")
                .loginProcessingUrl("/login")
                .successHandler(new AuthenticationSuccessHandler() {
                    @Override
                    public void onAuthenticationSuccess(HttpServletRequest request,
                                                        HttpServletResponse response,
                                                        Authentication authentication) throws IOException, ServletException {
                        response.sendRedirect(request.getContextPath() + "/index");
                    }
                })
                .failureHandler(new AuthenticationFailureHandler() {
                    @Override
                    public void onAuthenticationFailure(HttpServletRequest request,
                                                        HttpServletResponse response,
                                                        AuthenticationException e) throws IOException, ServletException {
                        request.setAttribute("error", e.getMessage());
                        request.getRequestDispatcher("/loginpage").forward(request, response);
                    }
                });
        //退出相关配置
        http.logout()
                .logoutUrl("/logout")
                .logoutSuccessHandler(new LogoutSuccessHandler() {
                    @Override
                    public void onLogoutSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {
                        response.sendRedirect(request.getContextPath() + "/index");
                    }
                });
        //授权配置
        http.authorizeRequests()
                .antMatchers("/letter").hasAnyAuthority("USER", "ADMIN")
                .antMatchers("/admin").hasAnyAuthority("ADMIN")
                //权限不足
                .and().exceptionHandling().accessDeniedPage("/denied");
        //验证码
        http.addFilterBefore(new Filter() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
                HttpServletRequest request = (HttpServletRequest) servletRequest;
                HttpServletResponse response = (HttpServletResponse) servletResponse;
                if (request.getServletPath().equals("/login")){
                    String code = request.getParameter("code");
                    if (code == null || !code.equals("1234")){
                        request.setAttribute("error", "验证码错误！");
                        request.getRequestDispatcher("/loginpage").forward(request, response);
                        return;
                    }
                }
                //请求继续执行
                filterChain.doFilter(request, response);
            }
        }, UsernamePasswordAuthenticationFilter.class);
        //记住我
        http.rememberMe()
                .tokenRepository(new InMemoryTokenRepositoryImpl())
                .tokenValiditySeconds(60 * 60 * 24)
                .userDetailsService(userService);
    }
}
