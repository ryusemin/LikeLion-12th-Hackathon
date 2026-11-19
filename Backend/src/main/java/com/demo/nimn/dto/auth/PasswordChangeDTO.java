package com.demo.nimn.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString(exclude = {"oldPassword", "newPassword", "emailVerificationToken"})
@AllArgsConstructor
@NoArgsConstructor
public class PasswordChangeDTO {
    private String email;
    private String oldPassword;
    private String newPassword;
    @Schema(description = "비밀번호 재설정에 필요한 이메일 인증 증명 토큰 (/email/check 응답의 verificationToken)")
    private String emailVerificationToken;
}
