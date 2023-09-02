package com.union.redisdemo.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.union.redisdemo.dto.Result;
import com.union.redisdemo.entity.Shop;
import com.union.redisdemo.mapper.ShopMapper;
import com.union.redisdemo.service.IShopService;
import com.union.redisdemo.utils.RedisConstants;
import com.union.redisdemo.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.*;


@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        //解决缓存穿透问题
        // Shop shop = getShopWithCacheThrough(id);

        //缓存重建、使用互斥锁，解决缓存击穿
        // Shop shop = getShopWithCacheHit(id);

        //热点数据，实现逻辑过期时间,解决缓存击穿
        Shop shop = queryShopWithLogicalExpire(id);

        if (shop == null){
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    // 前提条件，redis 中已经有了热点数据
    private Shop queryShopWithLogicalExpire(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY +id;
        String redisStr = stringRedisTemplate.opsForValue().get(key);

        if (StrUtil.isBlank(redisStr)){
            return null;
        }

        RedisData redisData = JSONUtil.toBean(redisStr, RedisData.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);

        //未过期
        if (expireTime.isAfter(LocalDateTime.now())){
            return shop;
        }

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        //已过期，实现缓存重建
        //获取锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = lockShop(lockKey);
        if (isLock){
            //开启线程池，执行缓存重建
            executorService.submit(()->{
                try {
                    this.saveShop2Redis(id, RedisConstants.CACHE_SHOP_TTL);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unlockShop(lockKey);
                }
            });
        }
        return shop;
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
            boolean isLock = lockShop(lockKey);
            if (!isLock){
                TimeUnit.MILLISECONDS.sleep(10);
                return getShopWithCacheHit(id);
            }
            //Todo 待执行方案： 缓存DoubleCheck，如果有了缓存数据，则无需缓存重建

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

    public void saveShop2Redis(Long id,Long expireTime){
        Shop shop = getById(id);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireTime));
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY+id, JSONUtil.toJsonStr(redisData));
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
