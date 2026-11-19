package com.demo.nimn.controller.auth;

import com.demo.nimn.dto.auth.TokenResponse;
import com.demo.nimn.exception.CustomException;
import com.demo.nimn.exception.ErrorCode;
import com.demo.nimn.exception.GlobalExceptionHandler;
import com.demo.nimn.service.auth.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Test the MVC contract; Security filters are outside this test's scope.
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void refreshReturnsAccessTokenInJsonAndRefreshTokenInHttpOnlyCookie() throws Exception {
        when(authService.reissueAccessToken("old-refresh"))
                .thenReturn(new TokenResponse("new-access", "new-refresh"));

        mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refreshToken", "old-refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().doesNotExist("Authorization"))
                .andExpect(cookie().value("refreshToken", "new-refresh"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().maxAge("refreshToken", 7 * 24 * 60 * 60));
    }

    @Test
    void missingRefreshTokenReturnsTheCommonUnauthorizedResponse() throws Exception {
        when(authService.reissueAccessToken(null))
                .thenThrow(new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Refresh token 없음"));
    }

    @Test
    void unexpectedServiceFailureDoesNotExposeItsInternalMessage() throws Exception {
        when(authService.reissueAccessToken("old-refresh"))
                .thenThrow(new IllegalStateException("private service failure"));

        mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refreshToken", "old-refresh")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 오류"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("private service failure"));
    }

    @Test
    void logoutClearsTheRefreshCookie() throws Exception {
        mockMvc.perform(post("/auth/logout").cookie(new Cookie("refreshToken", "old-refresh")))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andExpect(cookie().httpOnly("refreshToken", true));

        verify(authService).userLogout("old-refresh");
    }

    @Test
    void logoutWithoutRefreshTokenReturnsUnauthorized() throws Exception {
        doThrow(new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND))
                .when(authService).userLogout(null);

        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_NOT_FOUND"));
    }
}
