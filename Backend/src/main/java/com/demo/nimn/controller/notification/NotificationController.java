package com.demo.nimn.controller.notification;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.demo.nimn.dto.auth.CustomUserDetails;
import com.demo.nimn.dto.notification.NotificationDTO;
import com.demo.nimn.dto.notification.response.NotificationCountDTO;
import com.demo.nimn.enums.NotificationType;
import com.demo.nimn.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "알림 API", description = "결제 및 리뷰 알림 관리")
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @Operation(summary = "알림 조회", description = "이메일을 보내면 해당 사용자의 전체 알림을 조회")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "알림 조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationDTO.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "입력된 Email 형식이 잘못되었거나 유효하지 않은 값입니다.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content
            )
    })
    @GetMapping("/all")
    public ResponseEntity<List<NotificationDTO>> getAllNotifications(@AuthenticationPrincipal CustomUserDetails principal) {
        return  ResponseEntity.ok(notificationService.getAllNotificationsByUserEmail(principal.getUsername()));
    }

    @Operation(summary = "알림 읽음 처리", description = "알림 ID를 보내면 해당 알림을 읽음 처리")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "본인 소유가 아니거나 없는 데이터 (RESOURCE_NOT_FOUND)", content = @Content),
            @ApiResponse(
                    responseCode = "200",
                    description = "알림 읽은 처리 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationDTO.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "입력된 알림 ID 형식이 잘못되었거나 유효하지 않은 값입니다.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content
            )
    })
    @PatchMapping("/{notificationId}")
    public ResponseEntity<NotificationDTO> markAsRead(@AuthenticationPrincipal CustomUserDetails principal,
                                                      @Parameter(description = "알림 ID", example = "1")
                                                        @PathVariable Long notificationId) {
        return  ResponseEntity.ok(notificationService.markAsRead(notificationId, principal.getUsername()));
    }

    @Operation(summary = "모든 알림 읽음 처리", description = "로그인한 사용자의 모든 알림을 읽음 처리")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "모든 알림 읽음 처리 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationCountDTO.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "입력된 Email 형식이 잘못되었거나 유효하지 않은 값입니다.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content
            )
    })
    @PatchMapping
    public ResponseEntity<NotificationCountDTO> markAllAsRead(@AuthenticationPrincipal CustomUserDetails principal) {
        return  ResponseEntity.ok(notificationService.markAllAsRead(principal.getUsername()));
    }

    @Operation(summary = "안읽은 알림 갯수 조회", description = "로그인한 사용자의 안읽은 알림 갯수를 조회")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "안읽은 알림 갯수 조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationCountDTO.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "입력된 Email 형식이 잘못되었거나 유효하지 않은 값입니다.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content
            )
    })
    @GetMapping
    public ResponseEntity<NotificationCountDTO> countUnreadNotifications(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(notificationService.countUnreadNotifications(principal.getUsername()));
    }

    @Operation(summary = "테스트용")
    @GetMapping("/test")
    public ResponseEntity<NotificationCountDTO> test(@RequestParam NotificationType notificationType,
                                                     @RequestParam String userEmail,
                                                     @RequestParam String content,
                                                     @RequestParam Long dailyReviewId) {
        return ResponseEntity.ok(notificationService.testNotification(notificationType, userEmail, content, dailyReviewId));
    }
}
