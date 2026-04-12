package com.demo.nimn.service.auth;

public interface AuthService {
    public String reissueAccessToken(String refreshToken);

    public void userLogout(String refreshToken);
}
