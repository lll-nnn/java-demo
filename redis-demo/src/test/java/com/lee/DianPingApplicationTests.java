package com.lee;

import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import com.lee.entity.Shop;
import com.lee.service.IShopService;
import com.lee.utils.RedisData;
import com.lee.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.lee.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.lee.utils.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
class DianPingApplicationTests {

    @Autowired
    private RedisIdWorker redisIdWorker;

    private ExecutorService executorService = Executors.newFixedThreadPool(500);

    @org.junit.jupiter.api.Test
    public void name() {
        System.out.println(UUID.randomUUID(true).toString(true));
    }

    @Test
    public void testId() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                System.out.println(redisIdWorker.nextId("order"));
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            executorService.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }

    @Autowired
    private IShopService shopService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void insertRedisData() {
        List<Shop> list = shopService.list();
        for (Shop shop : list) {
            RedisData redisData = new RedisData();
            redisData.setData(shop);
            redisData.setExpireTime(LocalDateTime.now().plusSeconds(600));
            String json = JSONUtil.toJsonStr(redisData);
            redisTemplate.opsForValue().set(CACHE_SHOP_KEY + shop.getId(), json);
        }
    }

    @Test
    void loadShopData(){
        List<Shop> shops = shopService.list();
        Map<Long, List<Shop>> collect = shops.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        for (Map.Entry<Long, List<Shop>> entry : collect.entrySet()) {
            Long typeId = entry.getKey();
            List<Shop> shopList = entry.getValue();
            String key = SHOP_GEO_KEY + typeId;
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(shopList.size());
            for (Shop shop : shopList) {
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())
                ));
            }
            redisTemplate.opsForGeo().add(key, locations);
        }
    }
}
