package com.demo.nimn.config.security;

import com.demo.nimn.entity.auth.Users;
import com.demo.nimn.filter.JWTUtil;
import com.demo.nimn.repository.auth.UserRepository;
import com.demo.nimn.service.ai.AiService;
import com.demo.nimn.service.food.WeeklyFoodPlanService;
import com.demo.nimn.service.notification.NotificationService;
import com.siot.IamportRestClient.IamportClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 SecurityFilterChain(JWT 필터·인가 규칙·/api 경로 접두사)을 거쳐 공개·회원·관리자 API의 접근 권한을 확인한다 (#7).
 * dev·prod 프로필이 같은 규칙을 쓰는지 하위 클래스에서 각각 실행한다.
 */
@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:nimn-access-${random.uuid};MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=DAY",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
abstract class AbstractApiAccessControlTest {

    private static final String USER_EMAIL = "member@example.invalid";
    private static final String ADMIN_EMAIL = "admin@example.invalid";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JWTUtil jwtUtil;

    @MockitoBean private RedisConnectionFactory redisConnectionFactory;
    @MockitoBean private JavaMailSender javaMailSender;
    @MockitoBean private IamportClient iamportClient;
    @MockitoBean private AiService aiService;
    @MockitoBean private NotificationService notificationService;
    @MockitoBean private WeeklyFoodPlanService weeklyFoodPlanService;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        userRepository.save(Users.builder().name("member").email(USER_EMAIL)
                .password("unused").role("ROLE_USER").build());
        userRepository.save(Users.builder().name("admin").email(ADMIN_EMAIL)
                .password("unused").role("ROLE_ADMIN").build());
        userToken = jwtUtil.createAccessJwt(USER_EMAIL, "ROLE_USER");
        adminToken = jwtUtil.createAccessJwt(ADMIN_EMAIL, "ROLE_ADMIN");
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    // 공개 API: 토큰 없이도 Security에서 막지 않는다.

    @Test
    void anonymousCanReadPublicCatalogAndSignupChecks() throws Exception {
        mockMvc.perform(get("/api/foods/plans")).andExpect(status().isOk());
        mockMvc.perform(get("/api/review/summary")).andExpect(status().isOk());
        mockMvc.perform(get("/api/users/isExist").param("email", "nobody@example.invalid"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api-docs")).andExpect(status().isOk());
    }

    @Test
    void anonymousAuthEndpointsReachTheControllerInsteadOfTheSecurityBlock() throws Exception {
        // 쿠키 없는 재발급은 컨트롤러가 REFRESH_TOKEN_NOT_FOUND로 답한다. Security의 로그인 요구 응답이 아니다.
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_NOT_FOUND"));
    }

    @Test
    void anonymousImageDownloadIsNotBlockedBySecurity() throws Exception {
        // <img> 태그는 Authorization 헤더를 보내지 못하므로 이미지 조회는 공개다. 없는 이미지의 응답 코드는 이번 범위가 아니다.
        mockMvc.perform(get("/api/image/999999"))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }

    // 회원 API: 토큰이 없으면 컨트롤러에 닿기 전에 401이다.

    @Test
    void anonymousMemberApiIsUnauthorizedInsteadOfServerError() throws Exception {
        expectLoginRequired(get("/api/users"));
        expectLoginRequired(get("/api/notification/all").param("userEmail", USER_EMAIL));
        expectLoginRequired(get("/api/payment").param("purchaser", USER_EMAIL));
        expectLoginRequired(post("/api/order").contentType(MediaType.APPLICATION_JSON).content("{}"));
    }

    @Test
    void anonymousCannotUpdateAMemberAndTheDataStaysTheSame() throws Exception {
        expectLoginRequired(put("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + USER_EMAIL + "\",\"name\":\"changed\"}"));

        assertThat(userRepository.findByEmail(USER_EMAIL).getName()).isEqualTo("member");
    }

    @Test
    void memberCanReadOwnProfile() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(USER_EMAIL));
    }

    // 관리자 API: 비로그인은 401, 일반 회원은 403, 관리자는 통과한다.

    @Test
    void anonymousAdminApiIsUnauthorized() throws Exception {
        expectLoginRequired(get("/api/users/all"));
        expectLoginRequired(get("/api/payment/all"));
    }

    @Test
    void memberCannotUseAdminApis() throws Exception {
        expectForbidden(get("/api/users/all"));
        expectForbidden(get("/api/payment/all"));
        expectForbidden(delete("/api/payment/1"));
        expectForbidden(post("/api/foods/food").param("price", "1000"));
        expectForbidden(post("/api/foods/plans/weekly"));
        expectForbidden(delete("/api/image/1"));
        expectForbidden(get("/api/diet/daily/read/2026-01-01"));
        expectForbidden(get("/api/notification/test").param("notificationType", "MEAL"));
        expectForbidden(multipart("/api/image").file("imageFile", new byte[]{1}));
        expectForbidden(put("/api/image/1"));
    }

    @Test
    void memberCannotReachAdminGetApisWithHead() throws Exception {
        // Spring MVC는 HEAD를 @GetMapping 핸들러로 실행한다. GET에만 건 관리자 규칙을 HEAD로 우회하면 안 된다.
        expectForbidden(request(HttpMethod.HEAD, "/api/users/all"));
        expectForbidden(request(HttpMethod.HEAD, "/api/payment/all"));
        expectForbidden(request(HttpMethod.HEAD, "/api/diet/daily/read/2026-01-01"));
        expectForbidden(request(HttpMethod.HEAD, "/api/notification/test").param("notificationType", "MEAL"));
    }

    @Test
    void invalidTokenOnPublicApiIsRejectedSoTheClientCanRefreshOrRetryAsGuest() throws Exception {
        // 프론트는 이 401을 보고 재발급하고, 실패하면 토큰 없이 다시 보낸다 (#4).
        mockMvc.perform(get("/api/foods/plans").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    void adminCanUseAdminApis() throws Exception {
        mockMvc.perform(get("/api/users/all").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/payment/all").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        // 변경 계열 관리자 API도 Security에서 막지 않는다. 요청 내용 검증 결과는 이번 범위가 아니다.
        mockMvc.perform(delete("/api/image/999999").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }

    private void expectLoginRequired(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    private void expectForbidden(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}
