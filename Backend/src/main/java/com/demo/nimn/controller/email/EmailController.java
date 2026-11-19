package com.demo.nimn.controller.email;

import com.demo.nimn.dto.email.EmailDTO;
import com.demo.nimn.dto.email.EmailRequestDto;
import com.demo.nimn.service.email.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name="이메일 API", description = "이메일 인증 관리")
@RestController
@RequestMapping("/email")
public class EmailController {
    private final EmailVerificationService emailVerificationService;

    public EmailController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @Operation(summary = "인증 코드 전송", description = "목적(SIGNUP·PASSWORD_RESET)별 인증번호를 메일로 보낸다. 3분간 유효하며 60초 안에는 다시 보낼 수 없다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "이메일·목적 누락", content = @Content()),
            @ApiResponse(responseCode = "429", description = "재전송 대기 시간 이내", content = @Content()),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content())
    })
    @PostMapping("/send")
    public ResponseEntity<EmailDTO> mailSend(@RequestBody EmailRequestDto request) {
        emailVerificationService.sendCode(request.getEmail(), request.getPurpose());
        return ResponseEntity.ok(EmailDTO.builder()
                .email(request.getEmail())
                .isSuccess(true)
                .message("인증번호 전송 성공")
                .build());
    }

    @Operation(summary = "인증번호 일치여부 확인", description = "인증번호가 맞으면 가입·비밀번호 재설정에 사용할 일회용 verificationToken(10분 유효)을 발급한다. 최대 5회까지 확인할 수 있다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공, verificationToken 발급"),
            @ApiResponse(responseCode = "400", description = "인증번호 없음·만료 또는 요청 누락", content = @Content()),
            @ApiResponse(responseCode = "401", description = "인증번호 불일치", content = @Content()),
            @ApiResponse(responseCode = "429", description = "확인 시도 횟수 초과", content = @Content())
    })
    @PostMapping("/check")
    public ResponseEntity<EmailDTO> mailCheck(@RequestBody EmailRequestDto request) {
        String token = emailVerificationService.verifyCode(
                request.getEmail(), request.getPurpose(), String.valueOf(request.getCode()));
        return ResponseEntity.ok(EmailDTO.builder()
                .email(request.getEmail())
                .isSuccess(true)
                .message("이메일 인증이 완료되었습니다.")
                .verificationToken(token)
                .build());
    }
}
