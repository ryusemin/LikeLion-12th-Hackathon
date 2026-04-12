package com.demo.nimn.service.auth;

import java.util.Map;

public interface AuthService {
    String reissueAccessToken(String refreshToken);

    void userLogout(String refreshToken);

    Map<String, Object> loginSuccess(String email, String role);
}
