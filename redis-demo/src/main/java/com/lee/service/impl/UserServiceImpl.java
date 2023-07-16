package com.lee.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lee.dto.LoginFormDTO;
import com.lee.dto.Result;
import com.lee.dto.UserDTO;
import com.lee.entity.User;
import com.lee.mapper.UserMapper;
import com.lee.service.IUserService;
import com.lee.utils.RegexUtils;
import com.lee.utils.SystemConstants;
import com.lee.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.lee.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        if (!RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号无效！");
        }
        String code = RandomUtil.randomNumbers(6);//验证码
//        session.setAttribute("code", code);
//        session.setAttribute("phone", phone);
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //发送验证码
        log.debug("发送短信验证码成功，验证码为：" + code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        if (!RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号无效！");
        }
//        Object cacheCode = session.getAttribute("code");
//        Object cachePhone = session.getAttribute("phone");
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if (cacheCode == null || !cacheCode.equals(loginForm.getCode())){
            return Result.fail("验证码错误");
        }
//        if (cachePhone == null || !cachePhone.toString().equals(loginForm.getPhone())){
//            return Result.fail("手机号错误");
//        }
        User user = lambdaQuery().eq(User::getPhone, phone).one();
        if (user == null){
            user = new User();
            user.setPhone(phone);
            user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(6));
            save(user);
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor((key, value)->value.toString()));
        String token = UUID.randomUUID().toString(true);
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
//        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
//        session.removeAttribute("code");
//        session.removeAttribute("phone");
        return Result.ok(token);
    }

    @Override
    public Result logout(HttpServletRequest request) {
        String token = request.getHeader("authorization");
        String key = LOGIN_USER_KEY + token;
        stringRedisTemplate.delete(key);
        return Result.ok();
    }

    @Override
    public Result getUserById(Long id) {
        User user = getById(id);
        if (user == null){
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return Result.ok(userDTO);
    }

    @Override
    public Result sign() {
        UserDTO user = UserHolder.getUser();
        LocalDateTime now = LocalDateTime.now();
        String key = USER_SIGN_KEY + user.getId() + ":" + now.getYear() + ":" + now.getMonthValue();
        stringRedisTemplate.opsForValue()
                .setBit(key, now.getDayOfMonth() - 1, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        UserDTO user = UserHolder.getUser();
        LocalDateTime now = LocalDateTime.now();
        String key = USER_SIGN_KEY + user.getId() + ":" + now.getYear() + ":" + now.getMonthValue();
        int day = now.getDayOfMonth();
        List<Long> longs = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(day))
                        .valueAt(0)
        );
        if (longs == null || longs.isEmpty()){
            return Result.ok(0);
        }
        Long num = longs.get(0);
        if (num == null || num == 0){
            return Result.ok(0);
        }
        int res = 0;
        for (int i = 0; i < day; i++) {
            long tem = num & 1;
            if (tem == 1){
                res++;
            }
            num >>>= 1;//无符号右移
        }
        return Result.ok(res);
    }
}
