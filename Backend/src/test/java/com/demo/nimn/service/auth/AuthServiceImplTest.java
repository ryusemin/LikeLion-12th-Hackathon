package com.demo.nimn.service.auth;

import com.demo.nimn.dto.auth.TokenResponse;
import com.demo.nimn.entity.auth.Users;
import com.demo.nimn.exception.CustomException;
import com.demo.nimn.exception.ErrorCode;
import com.demo.nimn.filter.JWTUtil;
import com.demo.nimn.repository.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String EMAIL = "member@example.invalid";

    @Mock private JWTUtil jwtUtil;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserRepository userRepository;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(jwtUtil, refreshTokenService, userRepository);
    }

    @Test
    void missingRefreshTokenIsRejectedBeforeAnyDependencyCall() {
        assertThatThrownBy(() -> service.reissueAccessToken(null))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        verifyNoInteractions(jwtUtil, refreshTokenService, userRepository);
    }

    @Test
    void expiredRefreshTokenCannotIssueTokens() {
        when(jwtUtil.isExpired("expired-refresh")).thenReturn(true);

        assertThatThrownBy(() -> service.reissueAccessToken("expired-refresh"))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));

        verifyNoInteractions(refreshTokenService, userRepository);
        verify(jwtUtil, never()).createAccessJwt(anyString(), anyString());
    }

    @Test
    void aRevokedRefreshTokenCannotIssueTokens() {
        when(jwtUtil.getUsername("old-refresh")).thenReturn(EMAIL);
        when(refreshTokenService.getRefreshToken(EMAIL)).thenReturn(null);

        assertThatThrownBy(() -> service.reissueAccessToken("old-refresh"))
                .isInstanceOf(CustomException.class);

        verifyNoInteractions(userRepository);
        verify(jwtUtil, never()).createRefreshJwt(anyString());
    }

    @Test
    void aMismatchedTokenRevokesTheStoredTokenWithoutIssuingNewTokens() {
        when(jwtUtil.getUsername("old-refresh")).thenReturn(EMAIL);
        when(refreshTokenService.getRefreshToken(EMAIL)).thenReturn("current-refresh");

        assertThatThrownBy(() -> service.reissueAccessToken("old-refresh"))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));

        verify(refreshTokenService).deleteRefreshToken(EMAIL);
        verifyNoInteractions(userRepository);
        verify(jwtUtil, never()).createRefreshJwt(anyString());
    }

    @Test
    void aValidRefreshUsesCurrentDatabaseRoleAndReplacesTheStoredToken() {
        when(jwtUtil.getUsername("old-refresh")).thenReturn(EMAIL);
        when(refreshTokenService.getRefreshToken(EMAIL)).thenReturn("old-refresh");
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Users.builder().email(EMAIL).role("ROLE_USER").build());
        when(jwtUtil.createAccessJwt(EMAIL, "ROLE_USER")).thenReturn("new-access");
        when(jwtUtil.createRefreshJwt(EMAIL)).thenReturn("new-refresh");

        TokenResponse response = service.reissueAccessToken("old-refresh");

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        var updates = inOrder(refreshTokenService);
        updates.verify(refreshTokenService).deleteRefreshToken(EMAIL);
        updates.verify(refreshTokenService).saveRefreshToken(EMAIL, "new-refresh");
    }

    @Test
    void logoutRevokesTheStoredRefreshToken() {
        when(jwtUtil.getUsername("old-refresh")).thenReturn(EMAIL);

        service.userLogout("old-refresh");

        verify(refreshTokenService).deleteRefreshToken(EMAIL);
    }

    @Test
    void anUnparseableLogoutTokenDoesNotDeleteAnUnrelatedSession() {
        when(jwtUtil.getUsername("invalid-refresh")).thenThrow(new IllegalArgumentException());

        service.userLogout("invalid-refresh");

        verifyNoInteractions(refreshTokenService, userRepository);
    }
}
