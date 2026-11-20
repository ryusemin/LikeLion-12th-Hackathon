package com.demo.nimn.service.email;

import com.demo.nimn.exception.CustomException;
import com.demo.nimn.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static com.demo.nimn.enums.EmailVerificationPurpose.PASSWORD_RESET;
import static com.demo.nimn.enums.EmailVerificationPurpose.SIGNUP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final String EMAIL = "member@example.invalid";
    private static final String CODE_KEY = "email-code:PASSWORD_RESET:" + EMAIL;
    private static final String ATTEMPTS_KEY = "email-code-attempts:PASSWORD_RESET:" + EMAIL;
    private static final String COOLDOWN_KEY = "email-code-cooldown:PASSWORD_RESET:" + EMAIL;

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> values;
    @Mock private MailService mailService;

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(values);
        service = new EmailVerificationService(redisTemplate, mailService);
    }

    @Test
    void sendingStoresASixDigitCodeThatExpiresFromIssueTime() {
        when(values.setIfAbsent(COOLDOWN_KEY, "1", Duration.ofSeconds(60))).thenReturn(true);

        service.sendCode(EMAIL, PASSWORD_RESET);

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(values).set(eq(CODE_KEY), code.capture(), eq(Duration.ofMinutes(3)));
        assertThat(code.getValue()).matches("[1-9]\\d{5}");
        verify(redisTemplate).delete(ATTEMPTS_KEY);
        verify(mailService).sendCode(EMAIL, code.getValue());
    }

    @Test
    void resendingWithinTheCooldownIsRejectedWithoutMail() {
        when(values.setIfAbsent(COOLDOWN_KEY, "1", Duration.ofSeconds(60))).thenReturn(false);

        assertError(() -> service.sendCode(EMAIL, PASSWORD_RESET), ErrorCode.EMAIL_VERIFICATION_TOO_MANY_REQUESTS);

        verify(values, never()).set(anyString(), anyString(), any(Duration.class));
        verifyNoInteractions(mailService);
    }

    @Test
    void failedMailRemovesTheUnsentCodeAndCooldown() {
        when(values.setIfAbsent(COOLDOWN_KEY, "1", Duration.ofSeconds(60))).thenReturn(true);
        doThrow(new IllegalStateException("smtp down")).when(mailService).sendCode(eq(EMAIL), anyString());

        assertThatThrownBy(() -> service.sendCode(EMAIL, PASSWORD_RESET)).isInstanceOf(IllegalStateException.class);

        verify(redisTemplate).delete(List.of(CODE_KEY, COOLDOWN_KEY));
    }

    @Test
    void correctCodeIssuesAOneTimeProofBoundToEmailAndPurpose() {
        when(values.get(CODE_KEY)).thenReturn("123456");
        when(values.increment(ATTEMPTS_KEY)).thenReturn(1L);
        when(redisTemplate.delete(CODE_KEY)).thenReturn(true);

        String token = service.verifyCode(EMAIL, PASSWORD_RESET, "123456");

        assertThat(token).isNotBlank();
        verify(values).set("email-proof:" + token, "PASSWORD_RESET:" + EMAIL, Duration.ofMinutes(10));
        verify(redisTemplate).delete(ATTEMPTS_KEY);
    }

    @Test
    void wrongCodeIsRejectedAndIssuesNoProof() {
        when(values.get(CODE_KEY)).thenReturn("123456");
        when(values.increment(ATTEMPTS_KEY)).thenReturn(1L);

        assertError(() -> service.verifyCode(EMAIL, PASSWORD_RESET, "654321"), ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH);

        verify(values, never()).set(startsWith("email-proof:"), anyString(), any(Duration.class));
        verify(redisTemplate, never()).delete(CODE_KEY);
    }

    @Test
    void missingOrExpiredCodeIsRejected() {
        when(values.get(CODE_KEY)).thenReturn(null);

        assertError(() -> service.verifyCode(EMAIL, PASSWORD_RESET, "123456"), ErrorCode.EMAIL_VERIFICATION_CODE_NOT_FOUND);
    }

    @Test
    void codeIsDiscardedAfterTooManyAttempts() {
        when(values.get(CODE_KEY)).thenReturn("123456");
        when(values.increment(ATTEMPTS_KEY)).thenReturn(6L);

        assertError(() -> service.verifyCode(EMAIL, PASSWORD_RESET, "123456"), ErrorCode.EMAIL_VERIFICATION_TOO_MANY_REQUESTS);

        verify(redisTemplate).delete(List.of(CODE_KEY, ATTEMPTS_KEY));
        verify(values, never()).set(startsWith("email-proof:"), anyString(), any(Duration.class));
    }

    @Test
    void aCodeAlreadyConsumedByAConcurrentRequestIssuesNoSecondProof() {
        when(values.get(CODE_KEY)).thenReturn("123456");
        when(values.increment(ATTEMPTS_KEY)).thenReturn(1L);
        when(redisTemplate.delete(CODE_KEY)).thenReturn(false);

        assertError(() -> service.verifyCode(EMAIL, PASSWORD_RESET, "123456"), ErrorCode.EMAIL_VERIFICATION_CODE_NOT_FOUND);

        verify(values, never()).set(startsWith("email-proof:"), anyString(), any(Duration.class));
    }

    @Test
    void matchingProofIsConsumedAtomically() {
        when(values.getAndDelete("email-proof:token")).thenReturn("PASSWORD_RESET:" + EMAIL);

        service.consumeProof("token", EMAIL, PASSWORD_RESET);

        verify(values).getAndDelete("email-proof:token");
    }

    @Test
    void missingUsedOrMismatchedProofIsRejected() {
        assertError(() -> service.consumeProof(null, EMAIL, PASSWORD_RESET), ErrorCode.EMAIL_VERIFICATION_REQUIRED);
        assertError(() -> service.consumeProof(" ", EMAIL, PASSWORD_RESET), ErrorCode.EMAIL_VERIFICATION_REQUIRED);

        when(values.getAndDelete("email-proof:used")).thenReturn(null);
        assertError(() -> service.consumeProof("used", EMAIL, PASSWORD_RESET), ErrorCode.EMAIL_VERIFICATION_REQUIRED);

        when(values.getAndDelete("email-proof:other-email")).thenReturn("PASSWORD_RESET:other@example.invalid");
        assertError(() -> service.consumeProof("other-email", EMAIL, PASSWORD_RESET), ErrorCode.EMAIL_VERIFICATION_REQUIRED);

        when(values.getAndDelete("email-proof:signup")).thenReturn("SIGNUP:" + EMAIL);
        assertError(() -> service.consumeProof("signup", EMAIL, PASSWORD_RESET), ErrorCode.EMAIL_VERIFICATION_REQUIRED);
    }

    @Test
    void blankEmailOrMissingPurposeIsRejectedBeforeRedis() {
        assertError(() -> service.sendCode(" ", SIGNUP), ErrorCode.INVALID_INPUT);
        assertError(() -> service.sendCode(EMAIL, null), ErrorCode.INVALID_INPUT);
        assertError(() -> service.verifyCode(EMAIL, null, "123456"), ErrorCode.INVALID_INPUT);

        verifyNoInteractions(values, mailService);
    }

    private static void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
