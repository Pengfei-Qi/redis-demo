package com.union.redisdemo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class RedisDemoApplicationTests {
    //
    // private Jedis jedis;
    //
    // @BeforeEach
    // void setUp(){
    //     // jedis = new Jedis("10.136.106.77",30581);
    //     jedis = JedisConnectFactory.getJedis();
    //     jedis.auth("Nlkf@123%Redis");
    //     jedis.select(0);
    // }
    //
    // @Test
    // void contextLoads() {
    //     String keys = "source:department:name";
    //     String mset = jedis.mset(keys,  "hangman", "age", "55");
    //     System.out.println("set = " + mset);
    //     System.out.println("jedis.met(keys) = " + jedis.mget(keys));
    // }
    //
    // @Test
    // void testHset(){
    //     String keys = "source:depart-1:name";
    //     jedis.hset(keys,"blog","/sus");
    //     jedis.hset(keys,"photo","/heimapiaoliu");
    //     System.out.println("jedis.hgetAll(keys) = " + jedis.hgetAll(keys));
    // }
    //
    // @AfterEach
    // void turnDown(){
    //     if (jedis != null){
    //         jedis.close();
    //     }
    // }
}
