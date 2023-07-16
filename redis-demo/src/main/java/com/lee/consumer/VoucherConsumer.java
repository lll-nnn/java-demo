package com.lee.consumer;

import com.lee.entity.VoucherOrder;
import com.lee.service.ISeckillVoucherService;
import com.lee.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class VoucherConsumer {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @RabbitListener(queues = "voucher.save")
    public void saveVoucher(VoucherOrder voucherOrder){
        Long userId = voucherOrder.getUserId();
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean tryLock = lock.tryLock();
        if (!tryLock){
            log.error("不能重复下单");
            return;
        }
        try {
            Integer count = voucherOrderService.lambdaQuery().eq(VoucherOrder::getUserId, userId)
                    .eq(VoucherOrder::getVoucherId, voucherOrder.getVoucherId()).count();
            if (count > 0){
                log.error("不能重复下单");
                return;
            }
            boolean success = seckillVoucherService.update().setSql("stock = stock - 1")
                    .eq("voucher_id", voucherOrder.getVoucherId())
                    .gt("stock", 0)//乐观锁
                    .update();
            if (!success){
                log.error("卖完");
                return;
            }
            voucherOrderService.save(voucherOrder);
        } finally {
            lock.unlock();
        }
    }

}
