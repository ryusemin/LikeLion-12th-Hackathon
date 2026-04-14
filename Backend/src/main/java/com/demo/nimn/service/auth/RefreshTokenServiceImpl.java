package com.demo.nimn.service.auth;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService{

    private final RedisTemplate<String, String> redisTemplate;

    public RefreshTokenServiceImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveRefreshToken(String email, String refreshToken) {
        String key = "refresh:" + email;

        // 7일 TTL
        redisTemplate.opsForValue().set(key, refreshToken, 7, TimeUnit.DAYS);
    }

    public String getRefreshToken(String email) {
        return redisTemplate.opsForValue().get("refresh:" + email);
    }

    public void deleteRefreshToken(String email) {
        redisTemplate.delete("refresh:" + email);
    }
}
