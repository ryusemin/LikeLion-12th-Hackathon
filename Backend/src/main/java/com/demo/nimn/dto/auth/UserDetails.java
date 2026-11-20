package com.demo.nimn.dto.auth;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetails {
    @Schema(description = "회원 이름", example = "asd1234")
    private String name;
    @Schema(description = "회원가입 email", example = "asd1234@naver.com")
    private String email;
    @Schema(description = "회원가입 password", example = "asd1234")
    private String password;
    @Schema(description = "휴대폰 번호", example = "010-1234-1234")
    private String phoneNumber;
    @Schema(description = "주소", example = "대구광역시 수성구")
    private String roadAddress;
    @Schema(description = "상세주소", example = "은미아파트 101동 102호")
    private String detailAddress;
    @Schema(description = "회원가입 요청에 필요한 이메일 인증 증명 토큰 (/email/check 응답의 verificationToken)")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String emailVerificationToken;
}
