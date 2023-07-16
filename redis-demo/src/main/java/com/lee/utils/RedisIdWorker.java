package com.lee.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

//生成全局唯一id的工具类
@Component
public class RedisIdWorker {

    /**
     * 开始时间戳
     */
    private static final long BEGIN_TIMESTAMP = 1672531200L;
    /**
     * 序列号位数
     */
    private static final long COUNT_BITS = 32;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public long nextId(String prefixKey) {
        //时间戳
        long curSeconds = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long timestamp = curSeconds - BEGIN_TIMESTAMP;
        //序号
        String key = "icr:" + prefixKey + ":" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = redisTemplate.opsForValue().increment(key);
        //拼接返回
        return timestamp << COUNT_BITS | count;
    }

}
