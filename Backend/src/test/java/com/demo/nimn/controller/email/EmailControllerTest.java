package com.demo.nimn.controller.email;

import com.demo.nimn.exception.CustomException;
import com.demo.nimn.exception.ErrorCode;
import com.demo.nimn.exception.GlobalExceptionHandler;
import com.demo.nimn.service.email.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.demo.nimn.enums.EmailVerificationPurpose.PASSWORD_RESET;
import static com.demo.nimn.enums.EmailVerificationPurpose.SIGNUP;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmailControllerTest {

    private static final String EMAIL = "member@example.invalid";

    @Mock private EmailVerificationService emailVerificationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EmailController(emailVerificationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void sendPassesTheRequestedPurpose() throws Exception {
        mockMvc.perform(post("/email/send").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"purpose\":\"SIGNUP\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(emailVerificationService).sendCode(EMAIL, SIGNUP);
    }

    @Test
    void successfulCheckReturnsTheProofToken() throws Exception {
        when(emailVerificationService.verifyCode(EMAIL, PASSWORD_RESET, "123456")).thenReturn("proof-token");

        mockMvc.perform(post("/email/check").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"purpose\":\"PASSWORD_RESET\",\"code\":123456}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.verificationToken").value("proof-token"));
    }

    @Test
    void failedCheckUsesTheCommonErrorResponse() throws Exception {
        when(emailVerificationService.verifyCode(EMAIL, PASSWORD_RESET, "654321"))
                .thenThrow(new CustomException(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH));

        mockMvc.perform(post("/email/check").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"purpose\":\"PASSWORD_RESET\",\"code\":654321}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_CODE_MISMATCH"))
                .andExpect(jsonPath("$.verificationToken").doesNotExist());
    }

    @Test
    void sendCooldownIsReportedAsTooManyRequests() throws Exception {
        doThrow(new CustomException(ErrorCode.EMAIL_VERIFICATION_TOO_MANY_REQUESTS))
                .when(emailVerificationService).sendCode(EMAIL, SIGNUP);

        mockMvc.perform(post("/email/send").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"purpose\":\"SIGNUP\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_TOO_MANY_REQUESTS"));
    }
}
