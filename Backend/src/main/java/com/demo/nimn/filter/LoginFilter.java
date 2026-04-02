package com.demo.nimn.filter;

import com.demo.nimn.dto.auth.CustomUserDetails;
import com.demo.nimn.dto.auth.UserDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;

public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    //JWTUtil 주입
    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserDTO loginRequest;

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil, String customLoginUrl) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        setFilterProcessesUrl(customLoginUrl); // 커스텀 URL 경로 설정
    }

    @Override
    protected String obtainUsername(HttpServletRequest request) {
        return loginRequest.getEmail();
    }

    @Override
    protected String obtainPassword(HttpServletRequest request) {
        return loginRequest.getPassword();
    }


    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            loginRequest = objectMapper.readValue(request.getInputStream(), UserDTO.class);
        } catch (IOException e) {
            logger.error("Error parsing login request: ", e);
            throw new RuntimeException(e);
        }

        //클라이언트 요청에서 email, password 추출
        String email = obtainUsername(request);
        String password = obtainPassword(request);

        //스프링 시큐리티에서 email과 password를 검증하기 위해서는 token에 담아야 함
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password, null);

        //token에 담은 검증을 위한 AuthenticationManager로 전달
        return authenticationManager.authenticate(authToken);
    }

    //로그인 성공시 실행하는 메소드 (여기서 JWT를 발급하면 됨)
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) {

        try {
        //UserDetailsS
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

        String email = customUserDetails.getUsername();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();

        String role = auth.getAuthority();

        String accessToken = jwtUtil.createJwt(email, role);

        //헤더에 토큰 값을 넣을 때.
        response.addHeader("Authorization", "Bearer " + accessToken);

//        Cookie cookie = new Cookie("token", token);
//        cookie.setMaxAge(60 * 60 * 24); // 유효기간 설정(초), 상대시간
//        cookie.setPath("/");
//        cookie.setSecure(false);
//        cookie.setHttpOnly(false);
//        response.addCookie(cookie); // 응답에 쿠키 추가


        // JSON 형태로 응답 바디에 담기
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

            String jsonResponse = String.format(
                    "{\"isSuccess\": \"성공\", \"accessToken\": \"%s\"}",
                    accessToken
            );

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
        } catch (IOException e) {
            // IOException을 처리합니다.
            e.printStackTrace();
            // 적절한 오류 응답을 보냅니다.
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("{\"error\": \"Internal server error\"}");
                response.getWriter().flush();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    //로그인 실패시 실행하는 메소드
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        //로그인 실패시 401 응답 코드 반환
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = "{\"isSuccess\": \"실패\", \"message\": \"" + failed.getMessage() + "\"}";

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
        logger.info("로그인 실패");
    }
}
