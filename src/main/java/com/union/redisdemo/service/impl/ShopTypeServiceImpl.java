package com.union.redisdemo.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.union.redisdemo.dto.Result;
import com.union.redisdemo.entity.ShopType;
import com.union.redisdemo.mapper.ShopTypeMapper;
import com.union.redisdemo.service.IShopTypeService;
import com.union.redisdemo.utils.RedisConstants;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryList() {
        String shopTypeKey = RedisConstants.CACHE_SHOP_TYPE_KEY;
        ListOperations<String, String> ops = stringRedisTemplate.opsForList();
        List<String> range = ops.range(shopTypeKey, 0, -1);
        if (!CollectionUtils.isEmpty(range)){
            List<ShopType> shopTypeList = range.stream().map(s -> JSONUtil.toBean(s, ShopType.class)).sorted(Comparator.comparing(ShopType::getSort)).collect(Collectors.toList());
            return Result.ok(shopTypeList);
        }
        List<ShopType> list = query().orderByAsc("sort").list();
        if (list.isEmpty()){
            return Result.fail("找不到数据");
        }
        stringRedisTemplate.delete(shopTypeKey);
        ops.rightPushAll(shopTypeKey, list.stream().map(JSONUtil::toJsonStr).collect(Collectors.toList()));
        stringRedisTemplate.expire(shopTypeKey,RedisConstants.CACHE_SHOP_TYPE_TTL, TimeUnit.HOURS);
        return  Result.ok(list);
    }
}
