package com.demo.nimn.controller.auth;

import com.demo.nimn.entity.auth.Users;
import com.demo.nimn.exception.CustomException;
import com.demo.nimn.exception.ErrorCode;
import com.demo.nimn.repository.auth.UserRepository;
import com.demo.nimn.service.ai.AiService;
import com.demo.nimn.service.email.EmailVerificationService;
import com.demo.nimn.service.food.WeeklyFoodPlanService;
import com.demo.nimn.service.notification.NotificationService;
import com.siot.IamportRestClient.IamportClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs through the dev Security filter chain and /api path prefix (reset-password and signup are public, #7) so the
 * email proof is the only thing standing between an anonymous request and the account.
 */
@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:nimn-proof-test;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=DAY",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@ActiveProfiles({"test", "dev"})
@AutoConfigureMockMvc
class EmailProofSecurityTest {

    private static final String VICTIM = "victim@example.invalid";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private BCryptPasswordEncoder encoder;

    @MockitoBean private EmailVerificationService emailVerificationService;
    @MockitoBean private RedisConnectionFactory redisConnectionFactory;
    @MockitoBean private JavaMailSender javaMailSender;
    @MockitoBean private IamportClient iamportClient;
    @MockitoBean private AiService aiService;
    @MockitoBean private NotificationService notificationService;
    @MockitoBean private WeeklyFoodPlanService weeklyFoodPlanService;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void anonymousPasswordResetWithoutProofIsForbiddenAndKeepsThePassword() throws Exception {
        userRepository.save(Users.builder().name("victim").email(VICTIM)
                .password(encoder.encode("victim-password")).role("ROLE_USER").build());
        rejectAllProofs();

        mockMvc.perform(put("/api/users/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + VICTIM + "\",\"newPassword\":\"attacker-password\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_REQUIRED"));

        assertThat(encoder.matches("victim-password", userRepository.findByEmail(VICTIM).getPassword())).isTrue();
    }

    @Test
    void anonymousSignupWithoutProofIsForbiddenAndCreatesNoUser() throws Exception {
        rejectAllProofs();

        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"new\",\"email\":\"new@example.invalid\",\"password\":\"new-password\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_REQUIRED"));

        assertThat(userRepository.existsByEmail("new@example.invalid")).isFalse();
    }

    private void rejectAllProofs() {
        doThrow(new CustomException(ErrorCode.EMAIL_VERIFICATION_REQUIRED))
                .when(emailVerificationService).consumeProof(any(), anyString(), any());
    }
}
