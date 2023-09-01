package com.union.redisdemo.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.union.redisdemo.dto.Result;
import com.union.redisdemo.entity.Shop;
import com.union.redisdemo.mapper.ShopMapper;
import com.union.redisdemo.service.IShopService;
import com.union.redisdemo.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;


@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        //解决缓存穿透问题
        // Shop shop = getShopWithCacheThrough(id);

        //缓存重建、使用互斥锁，解决缓存击穿
        Shop shop = getShopWithCacheHit(id);

        if (shop == null){
            return Result.fail("商品不存在");
        }

        return Result.ok(shop);
    }

    public Shop getShopWithCacheThrough(Long id){
        String key = RedisConstants.CACHE_SHOP_KEY +id;

        String shopStr = stringRedisTemplate.opsForValue().get(key);

        /**
         * 字符串是否为非空白，非空白的定义如下：
         * 1. 不为 null
         * 2. 不为空字符串：""
         * 3. 不为空格、全角空格、制表符、换行符，等不可见字符
         */
        if (StrUtil.isNotBlank(shopStr)){
            return JSONUtil.toBean(shopStr, Shop.class);
        }
        // 说明：排除为 null 的情况，只剩下空字符串:""
        if (shopStr != null){
            return null;
        }
        Shop shop = getById(id);

        if (shop == null){
            stringRedisTemplate.opsForValue().set(key, "",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return shop;

    }

    public Shop getShopWithCacheHit(Long id){
        String key = RedisConstants.CACHE_SHOP_KEY +id;

        String shopStr = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(shopStr)){
            return JSONUtil.toBean(shopStr, Shop.class);
        }
        if (shopStr != null){
            return null;
        }
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        try {
            TimeUnit.SECONDS.sleep(1);
            boolean isLock = lockShop(lockKey);
            if (!isLock){
                TimeUnit.MILLISECONDS.sleep(10);
                return getShopWithCacheHit(id);
            }
            Shop shop = getById(id);
            if (shop == null){
                stringRedisTemplate.opsForValue().set(key, "",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return shop;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlockShop(lockKey);
        }
    }

    private boolean lockShop(String key){
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(aBoolean);
    }

    private void unlockShop(String key){
        stringRedisTemplate.delete(key);
    }

    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null){
            return Result.fail("商品id不能为空");
        }
        updateById(shop);
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY+ id);
        return null;
    }
}
