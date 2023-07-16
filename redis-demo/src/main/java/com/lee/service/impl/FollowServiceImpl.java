package com.lee.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lee.dto.Result;
import com.lee.dto.UserDTO;
import com.lee.entity.Follow;
import com.lee.entity.User;
import com.lee.mapper.FollowMapper;
import com.lee.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lee.service.IUserService;
import com.lee.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.lee.utils.RedisConstants.FOLLOW_PREFIX_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

    @Override
    public Result isFollower(Long followUserId) {
        UserDTO user = UserHolder.getUser();
        if (user == null)return Result.ok(false);
//        Integer count = lambdaQuery()
//                .eq(Follow::getFollowUserId, followUserId)
//                .eq(Follow::getUserId, user.getId()).count();
        Boolean member = stringRedisTemplate.opsForSet()
                .isMember(FOLLOW_PREFIX_KEY + user.getId(), followUserId.toString());
        return Result.ok(member);
    }

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        UserDTO user = UserHolder.getUser();
        if (user == null){
            return Result.fail("未登录!");
        }
        if (isFollow){
            Follow follow = new Follow();
            follow.setFollowUserId(followUserId);
            follow.setUserId(user.getId());
            follow.setCreateTime(LocalDateTime.now());
            boolean success = save(follow);
            if (success){
                stringRedisTemplate.opsForSet().add(FOLLOW_PREFIX_KEY + user.getId(), followUserId.toString());
            }
        }else {
//            remove(new QueryWrapper<Follow>()
//                    .eq("user_id", user.getId())
//                    .eq("follow_user_id", followUserId));
            boolean success = remove(new LambdaQueryWrapper<Follow>()
                    .eq(Follow::getUserId, user.getId())
                    .eq(Follow::getFollowUserId, followUserId));
            if (success){
                stringRedisTemplate.opsForSet().remove(FOLLOW_PREFIX_KEY + user.getId(), followUserId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result commonFollow(Long id) {
        UserDTO curUser = UserHolder.getUser();
        if (curUser == null){
            return Result.fail("未登录！");
        }
        String key1 = FOLLOW_PREFIX_KEY + curUser.getId();
        String key2 = FOLLOW_PREFIX_KEY + id;
        Set<String> common = stringRedisTemplate.opsForSet().intersect(key1, key2);
        if (common == null || common.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = common.stream().map(Long::valueOf).collect(Collectors.toList());
        List<User> users = userService.listByIds(ids);
        List<UserDTO> userDTOS = users.stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOS);
    }
}
