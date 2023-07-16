package com.lee.service.impl;

import cn.hutool.json.JSONUtil;
import com.lee.dto.Result;
import com.lee.entity.ShopType;
import com.lee.mapper.ShopTypeMapper;
import com.lee.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        String key = "shop:types";
        List<ShopType> res = new ArrayList<>();
        List<String> list = stringRedisTemplate.opsForList().range(key, 0, -1);
        if (list != null && !list.isEmpty()){
            list.forEach(s -> res.add(JSONUtil.toBean(s, ShopType.class)));
            return Result.ok(res);
        }
        List<ShopType> typeList = query().orderByAsc("sort").list();
        typeList.forEach(t -> stringRedisTemplate.opsForList().rightPush(key, JSONUtil.toJsonStr(t)));
        return Result.ok(typeList);
    }
}
