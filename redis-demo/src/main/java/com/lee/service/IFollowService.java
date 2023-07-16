package com.lee.service;

import com.lee.dto.Result;
import com.lee.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {

    Result isFollower(Long id);

    Result follow(Long followUserId, Boolean isFollow);

    Result commonFollow(Long id);
}
