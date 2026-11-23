package com.demo.nimn.controller.payment;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.demo.nimn.dto.auth.CustomUserDetails;
import com.demo.nimn.dto.payment.*;
import com.demo.nimn.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "결제 API", description = "아임포트 결제 검증 및 결제 내역 관리")
@RestController
@RequestMapping("/payment")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "결제 내역 생성", description = "아임포트 결제 UID와 주문 ID를 받아 결제를 검증하고 저장합니다.\n\n결제 금액 위변조 검증을 포함합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "결제 생성 및 검증 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "결제 실패 (결제 미완료, 결제금액 위변조 의심, 주문 내역 없음, 중복 결제 등)",
                    content = @Content
            )
    })
    @PostMapping("")
    public ResponseEntity<PaymentDTO> createPayment(@AuthenticationPrincipal CustomUserDetails principal,
                                                    @RequestBody PaymentRequestDTO paymentRequestDTO) {
        PaymentDTO created = paymentService.createImportPayment(paymentRequestDTO, principal.getUsername());
        return ResponseEntity.status(201).body(created);
    }

    @Operation(summary = "결제 내역 조회", description = "결제 ID를 통해 특정 결제 내역의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "결제 내역 조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "결제 내역을 찾을 수 없거나 서버 내부 오류",
                    content = @Content
            )
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDTO> getPaymentByPaymentId(@AuthenticationPrincipal CustomUserDetails principal,
                                                            @Parameter(description = "조회할 결제 ID", required = true, example = "PAY_20241217143055ABC123")
                                                                @PathVariable("paymentId") String paymentId) {
        PaymentDTO payment = paymentService.getPaymentByPaymentId(paymentId, principal.getUsername());
        return ResponseEntity.ok(payment);
    }

    @Operation(summary = "내 결제 내역 조회", description = "로그인한 사용자의 모든 결제 내역을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자별 결제 내역 조회 성공 (결제 내역이 없어도 빈 배열 반환)",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = PaymentDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content
            )
    })
    @GetMapping("")
    public ResponseEntity<List<PaymentDTO>> getPaymentByPurchaser(@AuthenticationPrincipal CustomUserDetails principal) {
        List<PaymentDTO> payments = paymentService.getPaymentByPurchaser(principal.getUsername());
        return ResponseEntity.ok(payments);
    }

    @Operation(summary = "전체 결제 내역 조회", description = "모든 결제 내역을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "결제 내역 조회 성공 (결제 내역이 없어도 빈 배열 반환)",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = PaymentDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content
            )
    })
    @GetMapping("/all")
    public ResponseEntity<List<PaymentDTO>> getAllPayments() {
        List<PaymentDTO> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(payments);
    }

    @Operation(summary = "결제 내역 삭제", description = "특정 결제 내역을 삭제합니다.\n\n아임포트와 연동된 실제 결제 취소는 동작하지 않습니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "결제 내역 삭제 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "boolean", example = "true"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content
            )
    })
    @DeleteMapping("/{paymentId}")
    public ResponseEntity<Boolean> deletePayment(@Parameter(description = "삭제할 결제 ID", required = true, example = "PAY_20241217143055ABC123")
                                                     @PathVariable("paymentId") String paymentId) {
        Boolean result = paymentService.deletePayment(paymentId);
        return ResponseEntity.ok(result);
    }
}