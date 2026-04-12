package com.demo.nimn.filter;

import com.demo.nimn.dto.auth.CustomUserDetails;
import com.demo.nimn.entity.auth.Users;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;

    public JWTFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (isPermitted(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        //request에서 Authorization 헤더를 찾음
        String token = null;

        // Authorization 헤더에서 토큰 추출
        String authorization = request.getHeader("Authorization");
        System.out.println("Authorization header: " + request.getHeader("Authorization"));

        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }

        //Authorization 헤더 검증
        if (token == null) {

            filterChain.doFilter(request, response);

            //조건이 해당되면 메소드 종료 (필수)
            return;
        }


        if (jwtUtil.isExpired(token)) {
            throw new RuntimeException("Access token expired");
        }

        //토큰에서 email과 role 획득
        String email = jwtUtil.getUsername(token);
        String role = jwtUtil.getRole(token);

        //userEntity를 생성하여 값 set
        Users userEntity = Users.builder()
                .email(email)
                .role(role)
                .build();

        //UserDetails에 회원 정보 객체 담기
        CustomUserDetails customUserDetails = new CustomUserDetails(userEntity);

        //스프링 시큐리티 인증 토큰 생성
        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        //세션에 사용자 등록
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }

    private static final List<String> WHITELIST = List.of(
            "/api/users/login",
            "/api/users/logout",
            "/api/users/isExist",
            "/api/users/signup",
            "/api/email",
            "/swagger-ui",
            "/docs",
            "/api-docs"
    );

    private boolean isPermitted(String uri) {
        return WHITELIST.stream().anyMatch(uri::startsWith);
    }
}
