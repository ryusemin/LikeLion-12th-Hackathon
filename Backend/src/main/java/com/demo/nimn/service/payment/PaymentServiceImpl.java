package com.demo.nimn.service.payment;

import com.demo.nimn.exception.ErrorCode;
import com.demo.nimn.exception.CustomException;
import com.demo.nimn.dto.diet.WeeklyDietDTO;
import com.demo.nimn.dto.payment.*;
import com.demo.nimn.entity.order.Order;
import com.demo.nimn.entity.payment.*;
import com.demo.nimn.repository.payment.PaymentRepository;
import com.demo.nimn.service.diet.DietService;
import com.demo.nimn.service.order.OrderService;
import com.demo.nimn.service.review.ReviewService;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.IamportClient;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final DietService dietService;
    private final ReviewService reviewService;
    private final IamportClient iamportClient;
    private final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              OrderService orderService,
                              DietService dietService,
                              ReviewService reviewService,
                              IamportClient iamportClient) {
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.dietService = dietService;
        this.reviewService = reviewService;
        this.iamportClient = iamportClient;
    }

    @Transactional
    @Override
    public PaymentDTO createImportPayment(PaymentRequestDTO request, String ownerEmail) {
        // 주문내역 조회: 본인 주문만 결제한다. 외부 결제 조회보다 먼저 확인한다 (#8).
        Order order = orderService.getOrder(request.getOrderId());
        if (!ownerEmail.equals(order.getPurchaser())) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        try {
            // 결제 단건 조회(아임포트)
            IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse = iamportClient.paymentByImpUid(request.getPaymentUid());

            // 결제 완료가 아니면
            if (!iamportResponse.getResponse().getStatus().equals("paid")) {
                logger.info("결제 미완료: {}", request.getPaymentUid());
                throw new RuntimeException("결제 미완료");
            }

            // DB에 저장된 결제 금액
            Long price = order.getTotalPrice();
            // 실 결제 금액
            int iamportPrice = iamportResponse.getResponse().getAmount().intValue();

            // 결제 금액 검증
            if (iamportPrice != price) {
                // 주문 취소
                orderService.cancelOrder(order.getId());
                // 결제금액 위변조로 의심되는 결제금액을 취소(아임포트)
                iamportClient.cancelPaymentByImpUid(new CancelData(iamportResponse.getResponse().getImpUid(), true, new BigDecimal(iamportPrice)));
                logger.info("결제금액 위변조 의심: {}", request.getPaymentUid());
                throw new RuntimeException("결제금액 위변조 의심");
            }
            // 결제 후 OrderStatus paid로 변경
            orderService.payOrder(order.getId());

            createWeeklyDietReviews(order);

            return createPayment(order, request.getPaymentUid());
        } catch (IamportResponseException e) {
            logger.error("아임포트 응답 처리 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.error("입출력 예외 발생: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void createWeeklyDietReviews(Order order) {
        try {
            WeeklyDietDTO weeklyDietDTO = dietService.getWeeklyDietById(order.getWeeklyDietId());

            reviewService.createWeeklyDietReviews(weeklyDietDTO);
        } catch (Exception e) {
            logger.error("Failed to create weekly diet reviews for order: {}", order.getId(), e);
        }
    }

    @Override
    public PaymentDTO createPayment(Order order, String paymentUid) {
        if (paymentRepository.existsByUid(paymentUid)) {
            throw new RuntimeException("이미 결제가 완료되었습니다.");
        }

        Payment payment = convertToPayment(order, paymentUid);
        paymentRepository.save(payment);

        return convertToPaymentDTO(payment);
    }

    @Override
    public PaymentDTO getPaymentByPaymentId(String paymentId, String ownerEmail) {
        Payment payment = paymentRepository.findById(paymentId)
                .filter(found -> ownerEmail.equals(found.getPurchaser()))
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        return convertToPaymentDTO(payment);
    }

    @Override
    public List<PaymentDTO> getPaymentByPurchaser(String purchaser) {
        List<Payment> paymentEntities = paymentRepository.findByPurchaser(purchaser);

        return convertToPaymentDTOList(paymentEntities);
    }

    @Override
    public List<String> getUnpaidPurchasersInWeek(LocalDate targetDate) {
        // targetDate가 포함된 주의 월요일 00:00:00 계산
        LocalDateTime startOfWeek = targetDate.with(java.time.DayOfWeek.MONDAY).atStartOfDay();
        // 해당 주의 일요일 23:59:59 계산
        LocalDateTime endOfWeek = startOfWeek.plusDays(6).withHour(23).withMinute(59).withSecond(59);

        return orderService.getUnpaidPurchasersThisWeek(startOfWeek, endOfWeek);
    }

    @Override
    public List<String> getPurchasersThisWeek() {
        return getUnpaidPurchasersInWeek(LocalDate.now());
    }

    @Override
    public List<PaymentDTO> getAllPayments(){
        List<Payment> payments = paymentRepository.findAllByOrderByCreatedAtDesc();

        return convertToPaymentDTOList(payments);
    }

    @Override
    public Boolean deletePayment(String paymentId) {
        paymentRepository.deleteById(paymentId);

        return !paymentRepository.existsById(paymentId);
    }

    private String generatePaymentId(String orderId) {
        return "PAY_" + orderId;
    }

    // Entity <-> DTO 변환 메소드들
    public Payment convertToPayment(Order order, String paymentUid) {
        return Payment.builder()
                .id(generatePaymentId(order.getId()))
                .uid(paymentUid)
                .purchaser(order.getPurchaser())
                .totalPrice(order.getTotalPrice())
                .weeklyDietId(order.getWeeklyDietId())
                .build();
    }

    public PaymentDTO convertToPaymentDTO(Payment payment) {
        return PaymentDTO.builder()
                .id(payment.getId())
                .purchaser(payment.getPurchaser())
                .weeklyDietId(payment.getWeeklyDietId())
                .uid(payment.getUid())
                .totalPrice(payment.getTotalPrice())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    public List<PaymentDTO> convertToPaymentDTOList(List<Payment> paymentEntities) {
        List<PaymentDTO> paymentDTOList = new ArrayList<>();

        for (Payment payment : paymentEntities) {
            paymentDTOList.add(convertToPaymentDTO(payment));
        }

        return paymentDTOList;
    }
}
