package com.demo.nimn.service.auth;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

public interface RefreshTokenService {

    public void saveRefreshToken(String email, String refreshToken);

    public String getRefreshToken(String email);

    public void deleteRefreshToken(String email);
}
