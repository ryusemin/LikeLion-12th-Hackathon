package com.demo.nimn.service.auth;

import com.demo.nimn.dto.auth.PasswordChangeDTO;
import com.demo.nimn.dto.auth.UserDTO;
import com.demo.nimn.dto.auth.UserDetails;
import com.demo.nimn.entity.auth.Users;
import com.demo.nimn.exception.CustomException;
import com.demo.nimn.exception.ErrorCode;
import com.demo.nimn.filter.JWTUtil;
import com.demo.nimn.repository.auth.UserRepository;
import com.demo.nimn.service.email.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static com.demo.nimn.enums.EmailVerificationPurpose.PASSWORD_RESET;
import static com.demo.nimn.enums.EmailVerificationPurpose.SIGNUP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String EMAIL = "member@example.invalid";
    private static final String CURRENT_PASSWORD = "current-password";

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private JWTUtil jwtUtil;
    @Mock private EmailVerificationService emailVerificationService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private UserServiceImpl service;
    private Users user;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(encoder, userRepository, refreshTokenService, jwtUtil, emailVerificationService);
        user = Users.builder().email(EMAIL).password(encoder.encode(CURRENT_PASSWORD)).role("ROLE_USER").build();
    }

    @Test
    void passwordResetWithoutAValidProofChangesNothing() {
        rejectProof("forged", PASSWORD_RESET);
        String before = user.getPassword();

        assertRequired(() -> service.updatePassword(new PasswordChangeDTO(EMAIL, null, "attacker-password", "forged")));

        assertThat(user.getPassword()).isEqualTo(before);
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void passwordResetDoesNotRevealTheCurrentPasswordWithoutAProof() {
        rejectProof(null, PASSWORD_RESET);

        // Without the proof check first, "중복" would confirm a guessed password.
        assertRequired(() -> service.updatePassword(new PasswordChangeDTO(EMAIL, null, CURRENT_PASSWORD, null)));
    }

    @Test
    void passwordResetWithAValidProofChangesPasswordAndRevokesRefreshToken() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(user);

        UserDTO result = service.updatePassword(new PasswordChangeDTO(EMAIL, null, "new-password", "proof"));

        verify(emailVerificationService).consumeProof("proof", EMAIL, PASSWORD_RESET);
        assertThat(result.getPassword()).isNull();
        assertThat(encoder.matches("new-password", user.getPassword())).isTrue();
        verify(refreshTokenService).deleteRefreshToken(EMAIL);
    }

    @Test
    void signupWithoutAValidProofCreatesNoUser() {
        rejectProof("forged", SIGNUP);

        assertRequired(() -> service.userSignup(signupRequest("forged")));

        verify(userRepository, never()).save(any());
    }

    @Test
    void signupWithAValidProofCreatesTheUser() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);

        service.userSignup(signupRequest("proof"));

        verify(emailVerificationService).consumeProof("proof", EMAIL, SIGNUP);
        verify(userRepository).save(any(Users.class));
    }

    private void rejectProof(String token, com.demo.nimn.enums.EmailVerificationPurpose purpose) {
        doThrow(new CustomException(ErrorCode.EMAIL_VERIFICATION_REQUIRED))
                .when(emailVerificationService).consumeProof(token, EMAIL, purpose);
    }

    private static UserDetails signupRequest(String token) {
        return UserDetails.builder()
                .name("member")
                .email(EMAIL)
                .password("new-password")
                .emailVerificationToken(token)
                .build();
    }

    private static void assertRequired(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_VERIFICATION_REQUIRED));
    }
}
