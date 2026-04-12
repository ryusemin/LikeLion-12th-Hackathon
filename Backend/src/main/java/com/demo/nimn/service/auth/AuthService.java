package com.demo.nimn.service.auth;

import com.demo.nimn.dto.auth.TokenResponse;

import java.util.Map;

public interface AuthService {
    TokenResponse reissueAccessToken(String refreshToken);

    void userLogout(String refreshToken);

    Map<String, Object> loginSuccess(String email, String role);
}
