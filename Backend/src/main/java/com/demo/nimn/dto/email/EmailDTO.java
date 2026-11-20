package com.demo.nimn.dto.email;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EmailDTO {
    private String email;
    private String message;
    private boolean isSuccess;
    @Schema(description = "인증 성공 시 발급되는 일회용 증명 토큰. 가입·비밀번호 재설정 요청에 함께 보낸다.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String verificationToken;
}
