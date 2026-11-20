package com.demo.nimn.service.email;

import com.demo.nimn.enums.EmailVerificationPurpose;
import com.demo.nimn.exception.CustomException;
import com.demo.nimn.exception.ErrorCode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 이메일 인증번호 발급·확인과, 인증 성공을 증명하는 일회용 토큰을 관리한다.
 * 가입·비밀번호 재설정 API는 이 토큰을 소비해야만 진행할 수 있다.
 */
@Service
public class EmailVerificationService {

    static final Duration CODE_TTL = Duration.ofMinutes(3);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    static final Duration PROOF_TTL = Duration.ofMinutes(10);
    static final int MAX_CHECK_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RedisTemplate<String, String> redisTemplate;
    private final MailService mailService;

    public EmailVerificationService(RedisTemplate<String, String> redisTemplate, MailService mailService) {
        this.redisTemplate = redisTemplate;
        this.mailService = mailService;
    }

    public void sendCode(String email, EmailVerificationPurpose purpose) {
        validate(email, purpose);
        String codeKey = key("email-code", purpose, email);
        String cooldownKey = key("email-code-cooldown", purpose, email);

        if (!Boolean.TRUE.equals(values().setIfAbsent(cooldownKey, "1", RESEND_COOLDOWN))) {
            throw new CustomException(ErrorCode.EMAIL_VERIFICATION_TOO_MANY_REQUESTS);
        }

        String code = String.valueOf(100_000 + RANDOM.nextInt(900_000));
        values().set(codeKey, code, CODE_TTL);
        redisTemplate.delete(key("email-code-attempts", purpose, email));

        try {
            mailService.sendCode(email, code);
        } catch (RuntimeException e) {
            redisTemplate.delete(List.of(codeKey, cooldownKey));
            throw e;
        }
    }

    /** 인증번호가 맞으면 email·purpose에 묶인 일회용 증명 토큰을 반환한다. */
    public String verifyCode(String email, EmailVerificationPurpose purpose, String code) {
        validate(email, purpose);
        String codeKey = key("email-code", purpose, email);
        String attemptsKey = key("email-code-attempts", purpose, email);

        String storedCode = values().get(codeKey);
        if (storedCode == null) {
            throw new CustomException(ErrorCode.EMAIL_VERIFICATION_CODE_NOT_FOUND);
        }

        Long attempts = values().increment(attemptsKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptsKey, CODE_TTL);
        }
        if (attempts == null || attempts > MAX_CHECK_ATTEMPTS) {
            redisTemplate.delete(List.of(codeKey, attemptsKey));
            throw new CustomException(ErrorCode.EMAIL_VERIFICATION_TOO_MANY_REQUESTS);
        }

        if (!storedCode.equals(code)) {
            throw new CustomException(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH);
        }
        // Only the request that actually deletes the code may issue a proof.
        if (!Boolean.TRUE.equals(redisTemplate.delete(codeKey))) {
            throw new CustomException(ErrorCode.EMAIL_VERIFICATION_CODE_NOT_FOUND);
        }
        redisTemplate.delete(attemptsKey);

        String token = UUID.randomUUID().toString();
        values().set("email-proof:" + token, purpose.name() + ":" + email, PROOF_TTL);
        return token;
    }

    /** 증명 토큰을 원자적으로 소비한다. 없거나 다른 email·purpose의 토큰이면 거부한다. */
    public void consumeProof(String token, String email, EmailVerificationPurpose purpose) {
        if (token == null || token.isBlank() || email == null || purpose == null) {
            throw new CustomException(ErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }
        String owner = values().getAndDelete("email-proof:" + token);
        if (!(purpose.name() + ":" + email).equals(owner)) {
            throw new CustomException(ErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }
    }

    private ValueOperations<String, String> values() {
        return redisTemplate.opsForValue();
    }

    private static void validate(String email, EmailVerificationPurpose purpose) {
        if (email == null || email.isBlank() || purpose == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private static String key(String prefix, EmailVerificationPurpose purpose, String email) {
        return prefix + ":" + purpose.name() + ":" + email;
    }
}
