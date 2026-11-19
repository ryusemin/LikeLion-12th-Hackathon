package com.demo.nimn.dto.email;


import com.demo.nimn.enums.EmailVerificationPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequestDto {
    private String email;
    @Schema(description = "인증번호 (확인 요청에서만 사용)", example = "123456")
    private int code;
    @Schema(description = "인증 목적", example = "SIGNUP")
    private EmailVerificationPurpose purpose;
}
