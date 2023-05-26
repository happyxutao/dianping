package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    /**
     * 开始时间戳
     */
    private static final long BEGIN_TIMESTAMP = 1684022400L;
    private static int COUNT_BITS=32;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 针对不同的业务，有不同的自增长
     * @param keyPrefix
     * @return
     */
    public long nextId(String keyPrefix){
        //1.生成时间戳
        LocalDateTime now=LocalDateTime.now();
        long nowSecond= now.toEpochSecond(ZoneOffset.UTC);
        long timestamp=nowSecond-BEGIN_TIMESTAMP;
        //2.生成序列号
        //2.1获取当前日期，精确到天
        String date=now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count=stringRedisTemplate.opsForValue().increment("icr:"+keyPrefix+":"+date);

        return timestamp<<COUNT_BITS|count;
    }

    /**
     * 获取当前时间对应的秒数
     * @param args
     */
//    public static void main(String[] args) {
//        LocalDateTime time=LocalDateTime.of(2023,5,14,0,0,
//                0);
//        long second=time.toEpochSecond(ZoneOffset.UTC);
//        System.out.println(second);
//    }
}
