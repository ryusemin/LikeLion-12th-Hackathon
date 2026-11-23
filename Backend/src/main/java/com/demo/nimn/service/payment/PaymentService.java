package com.demo.nimn.service.payment;

import com.demo.nimn.dto.payment.*;
import com.demo.nimn.entity.order.Order;

import java.time.LocalDate;
import java.util.List;

public interface PaymentService {
    PaymentDTO createImportPayment(PaymentRequestDTO request, String ownerEmail);
    PaymentDTO createPayment(Order order, String paymentUid);
    PaymentDTO getPaymentByPaymentId(String paymentId, String ownerEmail);
    List<PaymentDTO> getPaymentByPurchaser(String purchaser);
    List<String> getUnpaidPurchasersInWeek(LocalDate targetDate);
    List<String> getPurchasersThisWeek();
    List<PaymentDTO> getAllPayments();
    Boolean deletePayment(String paymentId);
}
