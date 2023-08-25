package com.union.redisdemo;

import com.union.redisdemo.dto.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

@SpringBootTest
public class RedisTemplateTests {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    void testRedis(){
        redisTemplate.opsForValue().set("aaa", "222");
        System.out.println("redisTemplate.opsForValue().get(\"aaa\") = " + redisTemplate.opsForValue().get("aaa"));
    }

    @Test
    void testObj() throws InterruptedException {
        User user = User.builder().name("zhangsan").age(30).companyAddress("上海市浦东新区2").build();
        ValueOperations<String,User> ops = redisTemplate.opsForValue();
        ops.set("com:neo:x",user);
        ops.set("com:neo:f",user,2, TimeUnit.MINUTES);
        TimeUnit.SECONDS.sleep(1);
        Boolean exist = redisTemplate.hasKey("com:neo:f");
        if (Boolean.TRUE.equals(exist)){
            User user1 = ops.get("com:neo:f");
            System.out.println("user1 = " + user1);
        }else {
            System.out.println("value exits is false");
        }

    }


}
