package com.union.redisdemo.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long timeout, TimeUnit timeUnit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), timeout, timeUnit);
    }

    //缓存穿透
    public <R,ID> R queryWithCacheThrough(
            String keyPrefix, ID id, Class<R> clazz, Function<ID, R> backData, Long timeout, TimeUnit unit) {
        String key = keyPrefix +id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)){
            return JSONUtil.toBean(json, clazz);
        }
        if (json != null){
            return null;
        }
        R r = backData.apply(id);
        if (r == null){
            stringRedisTemplate.opsForValue().set(key, "",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        this.set(key, r, timeout, unit);
        return r;
    }

    //使用互斥锁解决缓存击穿
    public <R,ID> R queryWithCacheHit(String keyPrefix, ID id, Class<R> clazz, Function<ID, R> backData, Long timeout, TimeUnit unit){
        String key = keyPrefix +id;

        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)){
            return JSONUtil.toBean(json, clazz);
        }
        if (json != null){
            return null;
        }
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        R r = null;
        try {
            boolean isLock = lockData(lockKey);
            if (!isLock){
                TimeUnit.MILLISECONDS.sleep(2);
                return queryWithCacheHit(keyPrefix,id,clazz,backData,timeout,unit);
            }
            r = backData.apply(id);
            if (r == null){
                stringRedisTemplate.opsForValue().set(key, "",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            this.set(key, r, timeout, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlock(lockKey);
        }
        return r;
    }

    private boolean lockData(String key){
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "9999", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(aBoolean);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    //使用逻辑过期解决缓存击穿
    public <R,ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> clazz,Function<ID, R> backData, Long timeout, TimeUnit unit) {
        String key = keyPrefix +id;
        String redisStr = stringRedisTemplate.opsForValue().get(key);

        if (StrUtil.isBlank(redisStr)){
            return null;
        }
        RedisData redisData = JSONUtil.toBean(redisStr, RedisData.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), clazz);

        //未过期
        if (expireTime.isAfter(LocalDateTime.now())){
            return r;
        }
        //已过期，实现缓存重建
        //获取锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = lockData(lockKey);
        if (isLock){
            //开启线程池，执行缓存重建
            executorService.submit(()->{
                try {
                    R r1 = backData.apply(id);
                    RedisData data = new RedisData();
                    data.setData(r1);
                    data.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(timeout)));
                    stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY+id, JSONUtil.toJsonStr(data));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unlock(lockKey);
                }
            });
        }
        return r;
    }
}
