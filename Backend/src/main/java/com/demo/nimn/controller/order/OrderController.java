package com.demo.nimn.controller.order;

import com.demo.nimn.exception.CustomException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.demo.nimn.dto.auth.CustomUserDetails;
import com.demo.nimn.dto.order.OrderDTO;
import com.demo.nimn.dto.order.OrderRequestDTO;
import com.demo.nimn.service.order.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "주문 API", description = "주간 식단 주문 생성 및 관리")
@RestController
@RequestMapping("/order")
public class OrderController {
    private final OrderService orderService;
    private final Logger logger = LoggerFactory.getLogger(OrderController.class);

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "주문 생성", description = "새로운 주문을 생성합니다.\n\n주문 생성 시 자동으로 총 금액이 계산되며 주문 ID가 자동 생성됩니다.\n\n생성된 주문으로 결제를 진행할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "본인 소유가 아니거나 없는 데이터 (RESOURCE_NOT_FOUND)", content = @Content),
            @ApiResponse(
                    responseCode = "201",
                    description = "주문 생성 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderDTO.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "주문 생성 실패 (주간 식단을 찾을 수 없거나 가격 계산 오류 등)",
                    content = @Content
            )
    })
    @PostMapping("")
    public ResponseEntity<OrderDTO> createOrder(@AuthenticationPrincipal CustomUserDetails principal,
                                                @RequestBody OrderRequestDTO orderRequestDTO) {
        try {
            OrderDTO orderDTO = OrderDTO.builder()
                    .purchaser(principal.getUsername())
                    .weeklyDietId(orderRequestDTO.getWeeklyDietId())
                    .build();

            OrderDTO created = orderService.createOrder(orderDTO);
            return ResponseEntity.status(201).body(created);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Order creation failed", e);
            throw new RuntimeException("주문 생성에 실패했습니다: " + e.getMessage());
        }
    }
}