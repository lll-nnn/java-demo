package com.lee.service;

import com.lee.dto.Result;
import com.lee.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result killVoucher(Long voucherId);

    Result getVoucherOrder(Long voucherId);
}
