package com.demo.nimn.service.notification;

import com.demo.nimn.exception.ErrorCode;
import com.demo.nimn.exception.CustomException;
import com.demo.nimn.dto.diet.DailyDietDTO;
import com.demo.nimn.dto.diet.FoodSelectionDTO;
import com.demo.nimn.dto.notification.NotificationDTO;
import com.demo.nimn.dto.notification.response.NotificationCountDTO;
import com.demo.nimn.dto.review.DailyDietReviewDTO;
import com.demo.nimn.entity.notification.Notification;
import com.demo.nimn.enums.NotificationType;
import com.demo.nimn.repository.notification.NotificationRepository;
import com.demo.nimn.service.diet.DietService;
import com.demo.nimn.service.payment.PaymentService;
import com.demo.nimn.service.review.ReviewService;
import com.demo.nimn.websocket.SessionManageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final NotificationRepository notificationRepository;
    private final SessionManageService sessionManageService;

    @Lazy
    private final DietService dietService;
    @Lazy
    private final PaymentService paymentService;
    @Lazy
    private final ReviewService reviewService;

    // 오늘의 유저별 식단 리스트
    private List<DailyDietDTO> dailyDietDTOList = new ArrayList<>();


    private void sendNotification(NotificationType notificationType,
                                  String userEmail,
                                  String content,
                                  Long dailyReviewId) {
        NotificationDTO notificationDTO = notificationRepository.save(
                Notification.builder()
                        .userEmail(userEmail)
                        .content(content)
                        .type(notificationType)
                        .sendTime(LocalDateTime.now())
                        .dailyReviewId(dailyReviewId)
                        .check(false)
                        .build()
        ).toNotificationDTO(
                notificationRepository.countByUserEmailAndCheckIsFalse(userEmail).orElse(0)
        );

        if (sessionManageService.isUserConnected(userEmail)) {
            try {
                simpMessagingTemplate.convertAndSendToUser(
                        userEmail,
                        "/queue/notification",
                        notificationDTO
                );
                log.info("Notification sent to {}: {}", userEmail, notificationDTO);
            } catch (Exception e) {
                log.error("Failed to send notification to {}", userEmail, e);
            }
        } else {
            log.warn("User {} not connected – notification queued.", userEmail);
        }
    }

    // 기존 testNotification 메서드 대체
    @Override
    public NotificationCountDTO testNotification(NotificationType notificationType,
                                                 String userEmail,
                                                 String content,
                                                 Long dailyReviewId) {
        NotificationDTO notificationDTO = notificationRepository.save(
                Notification.builder()
                        .userEmail(userEmail)
                        .content(content)
                        .type(notificationType)
                        .sendTime(LocalDateTime.now())
                        .dailyReviewId(dailyReviewId)
                        .check(false)
                        .build()
        ).toNotificationDTO(
                notificationRepository.countByUserEmailAndCheckIsFalse(userEmail).orElse(0)
        );

        try {
            // 개인 알림
            simpMessagingTemplate.convertAndSendToUser(
                    userEmail,
                    "/queue/notification",
                    notificationDTO
            );
            log.info("Test notification sent to {}: {}", userEmail, notificationDTO);
        } catch (Exception e) {
            log.error("Failed to send test notification to {}", userEmail, e);
        }

        return countUnreadNotifications(userEmail);
    }

    // 식단 알림 및 캐시 초기화 ( 매일 7시 )
    @Scheduled(cron = "0 0 7 ? * *")
    public void sendDailyDiet() {
        dailyDietDTOList = dietService.getByDate(LocalDate.now());

        for (DailyDietDTO dailyDietDTO : dailyDietDTOList) {
            StringBuilder content = new StringBuilder();
            content.append("오늘의 식단은 ");
            for (FoodSelectionDTO foodSelectionDTO : dailyDietDTO.getFoodSelections()) {
                content.append(foodSelectionDTO.getFoodTime())
                        .append(" ")
                        .append(foodSelectionDTO.getFoodMenu().getName())
                        .append(" ")
                        .append(foodSelectionDTO.getCount())
                        .append("개, ");
            }
            content.delete(content.length() - 3, content.length());
            content.append("입니다.");

            sendNotification(NotificationType.DIET,
                    dailyDietDTO.getUserEmail(),
                    content.toString(),
                    null);
        }
    }

    // 아침, 점심, 저녁 식사 알림
    @Scheduled(cron = "0 0 9,12,18 ? * *")
    public void sendDiet() {
        int hour = LocalDateTime.now().getHour();
        final String mealTime = hour == 9 ? "아침" : hour == 12 ? "점심" : "저녁";

        for (DailyDietDTO dailyDietDTO : dailyDietDTOList) {
            StringBuilder content = new StringBuilder();

            FoodSelectionDTO mealSelection = dailyDietDTO.getFoodSelections()
                    .stream()
                    .filter(mealSelectionDTO -> mealSelectionDTO.getFoodTime().equals(mealTime))
                    .findFirst()
                    .orElse(null);

            if (mealSelection != null) {
                content.append("오늘의 ")
                        .append(mealTime)
                        .append(" 식사는 ")
                        .append(mealSelection.getFoodMenu().getName())
                        .append(" ")
                        .append(mealSelection.getCount())
                        .append("개입니다.");

                sendNotification(NotificationType.DIET,
                        dailyDietDTO.getUserEmail(),
                        content.toString(),
                        null);
            }
        }
    }

    // 결제 알림
    @Scheduled(cron = "0 0 21 ? * MON")
    public void sendPayment() {
        List<String> users = paymentService.getPurchasersThisWeek();
        for (String userEmail : users) {
            sendNotification(NotificationType.PAYMENT,
                    userEmail,
                    "아직 이번주 결제가 완료되지 않았습니다.",
                    null);
        }
    }

    // 리뷰 알림
    @Scheduled(cron = "0 0 21 ? * *")
    public void sendReview() {
        List<DailyDietReviewDTO> reviewList = reviewService.getDailyDietReviewsByDate(LocalDate.now());
        for (DailyDietReviewDTO dailyReviewDTO : reviewList) {
            sendNotification(NotificationType.REVIEW,
                    dailyReviewDTO.getUserEmail(),
                    "오늘의 식단 리뷰에 참여해주세요~",
                    dailyReviewDTO.getId());
        }
    }

    // 안읽은 알림 갯수 조회
    @Override
    public NotificationCountDTO countUnreadNotifications(String userEmail) {
        return NotificationCountDTO.builder()
                .userEmail(userEmail)
                .count(notificationRepository.countByUserEmailAndCheckIsFalse(userEmail).orElse(0))
                .build();
    }

    // 확인으로 전환
    @Override
    public NotificationDTO markAsRead(Long notificationId, String ownerEmail) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(found -> ownerEmail.equals(found.getUserEmail()))
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        notification.setCheck(Boolean.TRUE);

        return notificationRepository.save(notification).toNotificationDTO(0);
    }

    // 유저의 모든 안읽은 메일 확인 전환
    @Override
    public NotificationCountDTO markAllAsRead(String userEmail) {
        List<Notification> notifications = notificationRepository.finaAllByNonChecked(userEmail);

        for (Notification notification : notifications) {
            notification.setCheck(Boolean.TRUE);
            notificationRepository.save(notification);
        }

        return NotificationCountDTO.builder()
                .userEmail(userEmail)
                .count(notifications.size())
                .build();
    }

    // 유저의 모든 알림 조회
    @Override
    public List<NotificationDTO> getAllNotificationsByUserEmail(String userEmail) {
        return notificationRepository.findAllByUserEmail(userEmail);
    }
}