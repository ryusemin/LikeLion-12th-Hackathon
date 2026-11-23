package com.demo.nimn.service.order;

import com.demo.nimn.exception.ErrorCode;
import com.demo.nimn.exception.CustomException;
import com.demo.nimn.dto.order.OrderDTO;
import com.demo.nimn.entity.diet.DailyDiet;
import com.demo.nimn.entity.diet.FoodSelection;
import com.demo.nimn.entity.diet.WeeklyDiet;
import com.demo.nimn.entity.order.Order;
import com.demo.nimn.repository.diet.WeeklyDietRepository;
import com.demo.nimn.repository.order.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final WeeklyDietRepository weeklyDietRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            WeeklyDietRepository weeklyDietRepository) {
        this.orderRepository = orderRepository;
        this.weeklyDietRepository = weeklyDietRepository;
    }

    @Override
    public OrderDTO createOrder(OrderDTO orderDTO) {
        // 주문할 수 있는 식단은 주문자 본인의 식단뿐이다 (#8).
        WeeklyDiet weeklyDiet = weeklyDietRepository.findById(orderDTO.getWeeklyDietId())
                .filter(diet -> orderDTO.getPurchaser().equals(diet.getUserEmail()))
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        orderDTO.setTotalPrice(calculateTotalPrice(weeklyDiet));

        Order order = convertToOrder(orderDTO);

        Order savedOrder = orderRepository.save(order);
        return convertToOrderDTO(savedOrder);
    }

    private Long calculateTotalPrice(WeeklyDiet weeklyDiet) {
        long totalPrice = 0;

        for (DailyDiet dailyDiet : weeklyDiet.getDailyDiets()) {

            for (FoodSelection foodSelection : dailyDiet.getFoodSelections()) {

                Long unitPrice = foodSelection.getFood().getPrice();

                totalPrice += unitPrice * foodSelection.getCount();
            }
        }

        return totalPrice;
    }

    @Override
    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Override
    public List<String> getUnpaidPurchasersThisWeek(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.findUnpaidPurchasersThisWeek(startDate, endDate);
    }

    @Override
    public void payOrder(String orderId) {
        Order order = getOrder(orderId);
        order.pay();
        orderRepository.save(order);
    }

    @Override
    public void cancelOrder(String orderId) {
        Order order = getOrder(orderId);
        order.cancel();
        orderRepository.save(order);
    }

    @Override
    public void deleteOrder(Order order) {
        orderRepository.delete(order);
    }

    // Entity <-> DTO 변환
    public Order convertToOrder(OrderDTO orderDTO) {
        return Order.builder()
                .purchaser(orderDTO.getPurchaser())
                .totalPrice(orderDTO.getTotalPrice())
                .weeklyDietId(orderDTO.getWeeklyDietId())
                .build();
    }

    public OrderDTO convertToOrderDTO(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .purchaser(order.getPurchaser())
                .totalPrice(order.getTotalPrice())
                .weeklyDietId(order.getWeeklyDietId())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}