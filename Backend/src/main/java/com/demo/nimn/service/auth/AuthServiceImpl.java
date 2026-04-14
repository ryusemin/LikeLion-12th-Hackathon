package com.demo.nimn.service.auth;

import com.demo.nimn.dto.auth.TokenResponse;
import com.demo.nimn.entity.auth.Users;
import com.demo.nimn.filter.JWTUtil;
import com.demo.nimn.repository.auth.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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
    public TokenResponse reissueAccessToken(String refreshToken) {

        if (refreshToken == null) {
            throw new RuntimeException("Refresh token 없음");
        }

        // 1. refresh 만료 체크
        if (jwtUtil.isExpired(refreshToken)) {
            throw new RuntimeException("Refresh token 만료");
        }

        // 2. email 추출
        String email = jwtUtil.getUsername(refreshToken);

        // 3. Redis에 저장된 값 조회
        String savedToken = refreshTokenService.getRefreshToken(email);

        // 🔥 4. 재사용 / 변조 감지
        if (savedToken == null) {
            throw new RuntimeException("로그인 정보 없음 (이미 로그아웃됨)");
        }

        if (!savedToken.equals(refreshToken)) {
            // 🔥 재사용 공격 감지 (중요)
            refreshTokenService.deleteRefreshToken(email); // 전체 세션 종료 느낌
            throw new RuntimeException("Refresh token 재사용 감지 (보안 위험)");
        }

        // 5. 유저 조회
        Users user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("유저 없음");
        }

        // 🔥 6. 기존 refresh 삭제 (Rotation 핵심)
        refreshTokenService.deleteRefreshToken(email);

        // 🔥 7. 새 토큰 발급
        String newAccessToken = jwtUtil.createAccessJwt(email, user.getRole());
        String newRefreshToken = jwtUtil.createRefreshJwt(email);

        // 🔥 8. Redis 저장
        refreshTokenService.saveRefreshToken(email, newRefreshToken);

        // 9. 응답 객체 반환
        return new TokenResponse(newAccessToken, newRefreshToken);
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

    @Override
    public Map<String, Object> loginSuccess(String email, String role) {

        String accessToken = jwtUtil.createAccessJwt(email, role);
        String refreshToken = jwtUtil.createRefreshJwt(email);

        refreshTokenService.saveRefreshToken(email, refreshToken);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);

        return result;
    }
}
