package com.demo.nimn.config.security;

import com.demo.nimn.filter.JwtAuthorizationFilter;
import com.demo.nimn.filter.JWTUtil;
import com.demo.nimn.filter.LoginFilter;
import com.demo.nimn.service.auth.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@Profile("prod")
public class SecurityConfigProd {

    //JWTUtil 주입
    private final JWTUtil jwtUtil;
    //AuthenticationManager가 인자로 받을 AuthenticationConfiguraion 객체 생성자 주입
    private final AuthenticationConfiguration authenticationConfiguration;
    private final AuthService authService;
    private final SecurityErrorHandler securityErrorHandler;

    public SecurityConfigProd(AuthenticationConfiguration authenticationConfiguration, JWTUtil jwtUtil, AuthService authService,
                              SecurityErrorHandler securityErrorHandler) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.jwtUtil = jwtUtil;
        this.authService = authService;
        this.securityErrorHandler = securityErrorHandler;
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        AuthenticationManager authenticationManager = authenticationConfiguration.getAuthenticationManager();

        LoginFilter loginFilter = new LoginFilter(
                authenticationManager,
                jwtUtil,
                "/api/auth/login",
                authService
        );

        //csrf disable
        http
                .csrf((auth) -> auth.disable());

        //From 로그인 방식 disable
        http
                .formLogin((auth) -> auth.disable());

        //http basic 인증 방식 disable
        http
                .httpBasic((auth) -> auth.disable());

        // CORS 설정
        http
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    corsConfig.setAllowedOrigins(java.util.List.of(
                            "http://localhost:3000",
                            "http://127.0.0.1:3000",
                            "https://nimn.store",
                            "http://nimn.store"
                    ));
                    corsConfig.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                    corsConfig.setAllowedHeaders(java.util.List.of("*"));
                    corsConfig.setAllowCredentials(true);
                    return corsConfig;
                }));
        // 공개·회원·관리자 API 접근 규칙 (#7)
        ApiAccessRules.apply(http, securityErrorHandler);



        //JWTFilter 등록
        http
                .addFilterBefore(new JwtAuthorizationFilter(jwtUtil), LoginFilter.class);

        //필터 추가 LoginFilter()는 인자를 받음 (AuthenticationManager() 메소드에 authenticationConfiguration 객체를 넣어야 함) 따라서 등록 필요
        //AuthenticationManager()와 JWTUtil 인수 전달
        http
                .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);

        //세션 설정
        http
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();

    }



}

