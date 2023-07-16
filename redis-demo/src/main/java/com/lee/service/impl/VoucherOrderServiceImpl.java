package com.lee.service.impl;

import com.lee.dto.Result;
import com.lee.entity.SeckillVoucher;
import com.lee.entity.VoucherOrder;
import com.lee.mapper.VoucherOrderMapper;
import com.lee.service.ISeckillVoucherService;
import com.lee.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lee.utils.RedisIdWorker;
import com.lee.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private RedisIdWorker redisIdWorker;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Resource
    private RabbitTemplate rabbitTemplate;

    public static final DefaultRedisScript<Long> SECKILL_LUA;
    static {
        SECKILL_LUA = new DefaultRedisScript<>();
        SECKILL_LUA.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_LUA.setResultType(Long.class);
    }
    //阻塞对列
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
    //单线程池
    private static final ExecutorService orderExecutor = Executors.newSingleThreadExecutor();

    @PostConstruct//当前类初始化完成后执行该方法
    private void init(){
        orderExecutor.submit(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable{
        @Override
        public void run() {
            while (true){
                try {
                    //获取阻塞队列中的订单信息
                    VoucherOrder voucherOrder = orderTasks.take();
                    //创建订单
                    Long userId = voucherOrder.getUserId();
                    RLock lock = redissonClient.getLock("lock:order:" + userId);
                    boolean tryLock = lock.tryLock();
                    if (!tryLock){
                        log.error("不能重复下单");
                        return;
                    }

                    try {
                        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
                        if (count > 0) {
                            log.error("不能重复下单！");
                            return;
                        }

                        boolean success = seckillVoucherService.update()
                                .setSql("stock = stock - 1")
                                .eq("voucher_id", voucherOrder.getVoucherId())
                                .gt("stock", 0)//乐观锁
                                .update();
                        if (!success) {
                            log.error("商品被抢光了！");
                            return;
                        }
                        save(voucherOrder);
                    } finally {
                        lock.unlock();
                    }

                } catch (Exception e) {
                    log.error("处理秒杀订单异常", e);
                }
            }
        }
    }

    @Override//优化秒杀业务
    public Result killVoucher(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("秒杀尚未开始！");
        }
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("秒杀已结束！");
        }
        //使用lua脚本
        Long userId = UserHolder.getUser().getId();
        Long execute = stringRedisTemplate.execute(
                SECKILL_LUA,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString());
        int exe = execute.intValue();
        if (exe != 0){
            return Result.fail(exe == 1 ? "库存不足" : "不能重复下单");
        }
        //异步保存订单信息
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        //保存到阻塞队列
//        orderTasks.add(voucherOrder);
        //rabbitmq发送消息，异步处理
        rabbitTemplate.convertAndSend("voucher.save", voucherOrder);
        return Result.ok(orderId);
    }

    /*@Override
    public Result killVoucher(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("秒杀尚未开始！");
        }
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("秒杀已结束！");
        }
        if (seckillVoucher.getStock() <= 0){
            return Result.fail("库存不足！");
        }
        Long userId = UserHolder.getUser().getId();
        //synchronized 加锁
//        synchronized (userId.toString().intern()){
//            //防止事务失效                获取事务的代理对象
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.getVoucherOrder(voucherId);
//        }
        //redis分布式锁
//        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
//        if (!lock.tryLock(500)){
//            return Result.fail("不能重复下单！");
//        }
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.getVoucherOrder(voucherId);
//        } finally {
//            //释放锁
//            lock.unlock();
//        }

        //redisson加锁
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        try {
            boolean tryLock = lock.tryLock(1, 10, TimeUnit.SECONDS);
            if (!tryLock){
                return Result.fail("不能重复下单!");
            }
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.getVoucherOrder(voucherId);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }*/

    @Transactional
    public Result getVoucherOrder(Long voucherId) {
        //一人一单
        Long userId = UserHolder.getUser().getId();


        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            return Result.fail("不能重复下单！");
        }

        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)//乐观锁
                .update();
        if (!success) {
            return Result.fail("商品被抢光了！");
        }
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        return Result.ok(orderId);
    }
}
