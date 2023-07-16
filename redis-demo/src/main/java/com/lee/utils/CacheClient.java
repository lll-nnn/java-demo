package com.lee.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.lee.utils.RedisConstants.*;

@Component
public class CacheClient {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    //逻辑过期时间
    public void setWithLogicExpire(String key, Object value, Long time, TimeUnit unit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));

        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 空值解决缓存穿透
     * @param keyPrefix redis key前缀
     * @param id    id
     * @param type  返回值类型
     * @param dbFunc    id查询
     * @param nullTime  空值的缓存时间
     * @param nullUnit  空值的缓存单位
     * @param cacheTime 非空值的缓存时间
     * @param cacheUnit 非空值的缓存单位
     * @return  type
     * @param <ID>  id类型
     * @param <R>   返回值类型
     */
    public <ID, R> R queryByPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFunc,
            Long nullTime,TimeUnit nullUnit, Long cacheTime, TimeUnit cacheUnit){
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)){
            return JSONUtil.toBean(json, type);
        }
        if (json != null){//为空
            return null;
        }
        R r = dbFunc.apply(id);
        if (r == null){
            //缓存空值防止缓存穿透
            stringRedisTemplate.opsForValue().set(key, "", nullTime, nullUnit);
            return null;
        }
        set(key, r, cacheTime, cacheUnit);
        return r;
    }

    //线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 逻辑过期解决缓存击穿
     * @param keyPrefix redis key前缀
     * @param lockPrefix    锁前缀
     * @param id    id
     * @param type  类型
     * @param dbFunc    数据库查询
     * @param time  缓存时间
     * @param unit  缓存时间单位
     * @return  r
     * @param <ID>  id类型
     * @param <R>   返回值类型
     */
    public <ID, R> R queryWithLogicExpire(
            String keyPrefix,String lockPrefix, ID id, Class<R> type, Function<ID, R> dbFunc,
            Long time, TimeUnit unit){
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        //未命中返回空
        if (StrUtil.isBlank(json)){
            return null;
        }
        //命中
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        R r = JSONUtil.toBean(data, type);
        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())){
            //未过期，直接返回
            return r;
        }
        //已过期，需要缓存重建
        //获取互斥锁
        String lockKey = lockPrefix + id;
        if (this.tryLock(lockKey)){
            //获取到锁      再次检查是否过期
            String js = stringRedisTemplate.opsForValue().get(key);
            RedisData redisData1 = JSONUtil.toBean(js, RedisData.class);
            LocalDateTime dateTime = redisData1.getExpireTime();
            if (dateTime.isAfter(LocalDateTime.now())){
                return JSONUtil.toBean((JSONObject) redisData1.getData(), type);
            }
            //再开一个线程进行缓存重建
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    //查数据库
                    R r1 = dbFunc.apply(id);
                    //写入缓存
                    this.setWithLogicExpire(key, r1, time, unit);
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    //释放锁
                    this.unlock(lockKey);
                }
            });
        }
        //未获取到锁
        return r;
    }

    /**
     * redis互斥锁
     * @param key   锁key
     * @return 是否获取到锁
     */
    public boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     * @param key   key
     */
    public void unlock(String key){
        stringRedisTemplate.delete(key);
    }

}
