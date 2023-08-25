// package com.union.redisdemo.config;
//
//
// import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
// import redis.clients.jedis.*;
//
// import java.time.Duration;
//
// public class JedisConnectFactory {
//
//     private static final JedisPool jedisPool;
//
//     static {
//         String host = "10.136.106.77";
//         int port = 32000;
//         GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();
//         poolConfig.setMaxIdle(8);
//         poolConfig.setMinIdle(0);
//         poolConfig.setMaxTotal(8);
//         poolConfig.setMaxWait(Duration.ofSeconds(1000));
//         jedisPool = new JedisPool(poolConfig,host,port);
//     }
//
//     public static Jedis getJedis(){
//         return jedisPool.getResource();
//     }
//
//
// }
