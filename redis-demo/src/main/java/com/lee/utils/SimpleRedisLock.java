package com.lee.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    private String name;

    private StringRedisTemplate stringRedisTemplate;

    private static final String LOCK_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    public static final DefaultRedisScript<Long> UNLOCK_LUA;
    static {
        UNLOCK_LUA = new DefaultRedisScript<>();
        UNLOCK_LUA.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_LUA.setResultType(Long.class);
    }

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        String curThread = ID_PREFIX + Thread.currentThread().getId();
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + name, curThread, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);//防止拆箱空指针
    }

    @Override
    public void unlock() {
        //使用lua脚本保证比较和释放的原子性
        stringRedisTemplate.execute(
                UNLOCK_LUA, Collections.singletonList(LOCK_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId());
    }
//    @Override
//    public void unlock() {
//        //比较当前线程是否是redis中的线程，防止误删其他线程的锁
//        String curThread = ID_PREFIX + Thread.currentThread().getId();
//        String thread = stringRedisTemplate.opsForValue().get(LOCK_PREFIX + name);
//        if (curThread.equals(thread)) {
//            stringRedisTemplate.delete(LOCK_PREFIX + name);
//        }
//    }
}
