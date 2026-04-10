package com.demo.nimn.controller.auth;

import com.demo.nimn.entity.auth.Users;
import com.demo.nimn.filter.JWTUtil;
import com.demo.nimn.repository.auth.UserRepository;
import com.demo.nimn.service.auth.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


@Tag(name="토큰 API", description = "리프레시 토큰 발급 및 조회")
@RestController
@RequestMapping("/token")
public class TokenController {

    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    public TokenController(JWTUtil jwtUtil, RefreshTokenService refreshTokenService, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
    }


    @Operation(summary = "리프레시 토큰 재발급", description = "재발급")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content()),
            @ApiResponse(responseCode = "403", description = "로그인 실패", content = @Content()),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content())
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {

        try {
            // 1. 쿠키에서 refreshToken 꺼내기
            String refreshToken = extractRefreshToken(request);

            if (refreshToken == null) {
                return ResponseEntity.status(401).body("Refresh token 없음");
            }

            // 2. 만료 체크
            if (jwtUtil.isExpired(refreshToken)) {
                return ResponseEntity.status(401).body("Refresh token 만료");
            }

            // 3. 이메일 추출
            String email = jwtUtil.getUsername(refreshToken);

            // 4. Redis 값 조회
            String savedToken = refreshTokenService.getRefreshToken(email);

            // 5. 비교
            if (savedToken == null || !savedToken.equals(refreshToken)) {
                return ResponseEntity.status(401).body("유효하지 않은 refresh token");
            }

            // 6. 새 access token 발급

            // DB 조회해서 최신 유저 가져오기
            Users user = userRepository.findByEmail(email);

            if (user == null) {
                throw new RuntimeException("유저 없음");
            }

            // 3. 최신 role 사용
            String role = user.getRole();
            String newAccessToken = jwtUtil.createAccessJwt(email, role);

            // 7. 응답
            Map<String, Object> body = new HashMap<>();
            body.put("accessToken", newAccessToken);
            body.put("isSuccess", "성공");

            return ResponseEntity.ok(body);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("서버 오류");
        }
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
