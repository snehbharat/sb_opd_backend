package com.sbpl.OPD.Auth.security;//package com.sbpl.OPD.security;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.util.concurrent.TimeUnit;
//
//@Service
//public class RateLimiterService {
//
//    @Autowired
//    private RedisTemplate<String, Object> redisTemplate;
//
//    private static final int MAX_REQUESTS_PER_MINUTE = 10; // Adjust based on requirements
//    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
//
//    public boolean isAllowed(String identifier) {
//        String key = RATE_LIMIT_PREFIX + identifier;
//        Long currentRequests = redisTemplate.opsForValue().increment(key);
//
//        if (currentRequests == 1) {
//            // Set expiration for 1 minute if this is the first request
//            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
//        }
//
//        return currentRequests <= MAX_REQUESTS_PER_MINUTE;
//    }
//
//    public long getRemainingRequests(String identifier) {
//        String key = RATE_LIMIT_PREFIX + identifier;
//        Long currentRequests = (Long) redisTemplate.opsForValue().get(key);
//
//        if (currentRequests == null) {
//            return MAX_REQUESTS_PER_MINUTE;
//        }
//
//        return Math.max(0, MAX_REQUESTS_PER_MINUTE - currentRequests);
//    }
//
//    public long getResetTime(String identifier) {
//        String key = RATE_LIMIT_PREFIX + identifier;
//        return redisTemplate.getExpire(key);
//    }
//}