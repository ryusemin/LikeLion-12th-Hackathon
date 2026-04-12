package com.demo.nimn.controller.auth;

import com.demo.nimn.dto.auth.TokenResponse;
import com.demo.nimn.dto.auth.UserDTO;
import com.demo.nimn.service.auth.AuthService;
import com.demo.nimn.service.auth.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


@Tag(name="인증 API", description = "유저 인증 관리")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "로그인", description = "로그인에 성공하면 응답 Header에 jwt를 포함")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content()),
            @ApiResponse(responseCode = "403", description = "로그인 실패", content = @Content()),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content())
    })
    @PostMapping("/login")
    public void login (@RequestBody UserDTO userDTO) {
    }

    @Operation(summary = "로그아웃", description = "로그아웃을 진행, 쿠키에 담긴 jwt 토큰을 삭제합니다.", security = @SecurityRequirement(name = "accessToken"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content()),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content())
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    HttpServletResponse response) {

        String refreshToken = extractRefreshToken(request);

        authService.userLogout(refreshToken);

        // 쿠키 삭제
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "엑세스 토큰 재발급", description = "쿠키의 리프레시 토큰으로 엑세스 토큰 재발급")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content()),
            @ApiResponse(responseCode = "403", description = "로그인 실패", content = @Content()),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content())
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = extractRefreshToken(request);

        TokenResponse tokenResponse = authService.reissueAccessToken(refreshToken);

        // 🔥 refresh 쿠키 갱신 (중요)
        Cookie cookie = new Cookie("refreshToken", tokenResponse.getRefreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // 배포 시 true
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(cookie);

        Map<String, Object> body = new HashMap<>();
        body.put("accessToken", tokenResponse.getAccessToken());
        body.put("isSuccess", "성공");

        return ResponseEntity.ok(body);
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
