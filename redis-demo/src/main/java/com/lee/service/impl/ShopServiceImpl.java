package com.lee.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lee.dto.Result;
import com.lee.entity.Shop;
import com.lee.mapper.ShopMapper;
import com.lee.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lee.utils.CacheClient;
import com.lee.utils.RedisData;
import com.lee.utils.SystemConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static com.lee.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        //缓存穿透
//        Shop shop = queryByPassThrough(id);
//        Shop shop = cacheClient.queryByPassThrough(CACHE_SHOP_KEY, id, Shop.class, (i) -> getById(i),
//                CACHE_NULL_TTL, TimeUnit.MINUTES, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //互斥锁解决缓存击穿
//        Shop shop = queryByMutex(id);
        //逻辑过期解决缓存击穿
//        Shop shop = queryWithLogicExpire(id);
        Shop shop = cacheClient
                .queryWithLogicExpire
                        (CACHE_SHOP_KEY, LOCK_SHOP_KEY, id,
                                Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        if (shop == null){
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    //线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    //逻辑过期解决缓存穿透
    public Shop queryWithLogicExpire(Long id){
        String key = CACHE_SHOP_KEY + id;
        String cacheShop = stringRedisTemplate.opsForValue().get(key);
        //未命中返回空
        if (StrUtil.isBlank(cacheShop)){
            return null;
        }
        //命中
        RedisData redisData = JSONUtil.toBean(cacheShop, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        Shop shop = JSONUtil.toBean(data, Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())){
            //未过期，直接返回
            return shop;
        }
        //已过期，需要缓存重建
        //获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        if (tryLock(lockKey)){
            //获取到锁      再次检查是否过期
            String cacheShop2 = stringRedisTemplate.opsForValue().get(key);
            RedisData redisData1 = JSONUtil.toBean(cacheShop, RedisData.class);
            LocalDateTime dateTime = redisData1.getExpireTime();
            if (dateTime.isAfter(LocalDateTime.now())){
                return JSONUtil.toBean((JSONObject) redisData1.getData(), Shop.class);
            }
            //再开一个线程进行缓存重建
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    this.saveShop2Redis(id, 1800L);
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    //释放锁
                    unlock(lockKey);
                }
            });
        }
        //未获取到锁
        return shop;
    }

//互斥锁解决缓存击穿
    public Shop queryByMutex(Long id){
        String key = CACHE_SHOP_KEY + id;
        String cacheShop = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(cacheShop)){
            return JSONUtil.toBean(cacheShop, Shop.class);
        }
        if (cacheShop != null){
            return null;
        }
        //实现缓存重建
        //获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try{
            if (!tryLock(lockKey)){
                Thread.sleep(50);
                return queryByMutex(id);
            }
            //获取到锁后再次查询缓存
            String cacheShop2 = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(cacheShop2)){
                return JSONUtil.toBean(cacheShop2, Shop.class);
            }
            shop = getById(id);
            if (shop == null){
                //缓存空值防止缓存穿透
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //加上过期时间
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            //释放锁
            unlock(lockKey);
        }
        return shop;
    }

    public Shop queryByPassThrough(Long id){
        String key = CACHE_SHOP_KEY + id;
        String cacheShop = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(cacheShop)){
            return JSONUtil.toBean(cacheShop, Shop.class);
        }
        if (cacheShop != null){
            return null;
        }
        Shop shop = getById(id);
        if (shop == null){
            //缓存空值防止缓存穿透
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //加上过期时间
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return shop;
    }

    //互斥锁防止缓存击穿
    public boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    public void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    public void saveShop2Redis(Long id, Long expireSeconds){
        Shop shop = getById(id);
        RedisData data = new RedisData();
        data.setData(shop);
        data.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(data));
    }

    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null){
            return Result.fail("店铺id为空");
        }
        //先更新数据库，再删缓存
        updateById(shop);
        String key = CACHE_SHOP_KEY + id;
        stringRedisTemplate.delete(key);
        return Result.ok();
    }

//    @Override
//    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
//        Page<Shop> page = lambdaQuery()
//                    .eq(Shop::getTypeId, typeId)
//                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
//        return Result.ok(page.getRecords());
//    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        if (x == null || y == null){
            Page<Shop> page = lambdaQuery()
                    .eq(Shop::getTypeId, typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }
        //有坐标
        String key = SHOP_GEO_KEY + typeId;
        //分页参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        //georadius
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().radius(
                key,
                new Circle(x, y, 5000),
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().limit(end)
        );

        //查询redis 排序 分页         //geosearch
//        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().search(
//                key,
//                GeoReference.fromCoordinate(x, y),
//                new Distance(5000),
//                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
//        );
        if (results == null){
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();
        if (content.size() <= from){
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = new ArrayList<>(content.size());
        Map<String, Distance> distanceMap = new HashMap<>(content.size());
        //截取from ~ end的部分
        content.stream().skip(from).forEach(r ->{
            String shopId = r.getContent().getName();
            ids.add(Long.valueOf(shopId));
            distanceMap.put(shopId, r.getDistance());
        });
        //根据id查询shop 保证顺序
        String idString = StrUtil.join(",", ids);
        List<Shop> list = lambdaQuery().in(Shop::getId, ids).last("order by field(id, " + idString + ")").list();
        for (Shop shop : list) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        return Result.ok(list);
    }
}
