package com.demo.nimn.config.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * dev·prod가 함께 쓰는 API 접근 규칙 (#7).
 * 컨트롤러 경로에는 WebConfig가 /api 접두사를 붙이므로 규칙도 /api를 포함한 실제 요청 경로로 적는다.
 * 위에서부터 처음 맞는 규칙을 적용하므로, 관리자 규칙을 공개 규칙보다 먼저 둔다.
 * 여기서는 로그인 여부와 역할만 판단한다. 본인 데이터인지(소유자) 검증은 #8에서 다룬다.
 */
final class ApiAccessRules {

    private ApiAccessRules() {
    }

    static void apply(HttpSecurity http, SecurityErrorHandler errorHandler) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                // 오류 응답 전달과 API 문서
                .requestMatchers("/error").permitAll()
                .requestMatchers("/docs", "/docs/**", "/swagger-ui/**", "/swagger-ui.html",
                        "/api-docs", "/api-docs/**").permitAll()
                // SockJS 연결 수립은 Authorization 헤더를 보낼 수 없다. STOMP CONNECT 인증·구독 권한은 #10에서 다룬다.
                .requestMatchers("/ws/**").permitAll()

                // 관리자 전용 경로: 전체 회원·결제 조회, 날짜별 전체 식단, 알림 테스트.
                // 메서드를 지정하지 않는다. Spring MVC는 HEAD를 @GetMapping 핸들러로 실행하므로 GET만 막으면 HEAD로 우회된다.
                .requestMatchers("/api/users/all", "/api/payment/all",
                        "/api/diet/daily/read/*", "/api/notification/test").hasRole("ADMIN")
                // 관리자: 같은 경로를 회원·공개와 나눠 쓰는 변경 API(결제 삭제, 음식·식단 선택지·이미지 관리)
                .requestMatchers(HttpMethod.DELETE, "/api/payment/*", "/api/image/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/foods/food", "/api/foods/plans/weekly",
                        "/api/image").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/image/*").hasRole("ADMIN")

                // 공개: 로그인·재발급·로그아웃, 가입·이메일 인증·비밀번호 재설정
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh",
                        "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users/signup", "/api/email/send",
                        "/api/email/check").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/isExist").permitAll()
                // 로그인 없이 쓰는 재설정은 이메일 인증 증명을 서버가 확인한다 (#6).
                .requestMatchers(HttpMethod.PUT, "/api/users/reset-password").permitAll()
                // 공개: 음식·식단 선택지·리뷰 요약 조회, 이미지(<img>는 Authorization 헤더를 보낼 수 없다)
                .requestMatchers(HttpMethod.GET, "/api/foods/plans", "/api/foods/plans/weekly",
                        "/api/foods/food/*", "/api/review/summary", "/api/review/summary/**",
                        "/api/image/*").permitAll()

                // 그 외는 모두 로그인한 회원만 사용한다.
                .anyRequest().authenticated());

        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(errorHandler)
                .accessDeniedHandler(errorHandler));
    }
}
