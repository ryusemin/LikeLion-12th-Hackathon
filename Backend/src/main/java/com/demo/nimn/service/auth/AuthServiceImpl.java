package com.demo.nimn.service.auth;

import com.demo.nimn.entity.auth.Users;
import com.demo.nimn.filter.JWTUtil;
import com.demo.nimn.repository.auth.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService{

    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    public AuthServiceImpl(JWTUtil jwtUtil, RefreshTokenService refreshTokenService, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
    }

    @Override
    public String reissueAccessToken(String refreshToken) {

        if (refreshToken == null) {
            throw new RuntimeException("Refresh token 없음");
        }

        if (jwtUtil.isExpired(refreshToken)) {
            throw new RuntimeException("Refresh token 만료");
        }

        String email = jwtUtil.getUsername(refreshToken);

        String savedToken = refreshTokenService.getRefreshToken(email);

        if (savedToken == null || !savedToken.equals(refreshToken)) {
            throw new RuntimeException("유효하지 않은 refresh token");
        }

        Users user = userRepository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("유저 없음");
        }

        return jwtUtil.createAccessJwt(email, user.getRole());
    }

    @Override
    public void userLogout(String refreshToken) {

        if (refreshToken == null) {
            throw new RuntimeException("Refresh token 없음");
        }

        // 🔥 email 기반 구조니까 email 추출 필요
        String email = jwtUtil.getUsername(refreshToken);

        // Redis 삭제
        refreshTokenService.deleteRefreshToken(email);
    }
}
