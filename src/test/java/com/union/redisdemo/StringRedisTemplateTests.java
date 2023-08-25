package com.union.redisdemo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.union.redisdemo.vo.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class StringRedisTemplateTests {


    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testRedis(){
        stringRedisTemplate.opsForValue().set("com:neo:c", "222");
        System.out.println("stringRedisTemplate.opsForValue().get(\"aaa\") = " + stringRedisTemplate.opsForValue().get("com:neo:c"));
    }

    @Test
    void testStringRedis() throws Exception {
        User user = User.builder().name("zhaoliu").age(66).companyAddress("背景闵行区").build();
        String userJson = jsonMapper.writeValueAsString(user);
        ValueOperations<String, String> forValue = stringRedisTemplate.opsForValue();
        forValue.set("com:neo:d",userJson,20,TimeUnit.SECONDS);
        TimeUnit.SECONDS.sleep(2);
        String s = forValue.get("com:neo:d");
        User user1 = jsonMapper.readValue(s, User.class);
        System.out.println("user1 = " + user1);

    }

    @Test
    void testHash() throws JsonProcessingException {
        User user = User.builder().name("七七").age(27).companyAddress("山西鹿城区").build();
        stringRedisTemplate.opsForHash().put("com:neo:e","company","万州科技");
        String userStr = jsonMapper.writeValueAsString(user);
        stringRedisTemplate.opsForHash().put("com:neo:e","info",userStr);
        System.out.println("------------------------");
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries("com:neo:e");
        System.out.println(entries);
    }
}
