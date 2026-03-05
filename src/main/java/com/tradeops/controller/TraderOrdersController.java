package com.tradeops.controller;

import com.tradeops.model.entity.Order;
import com.tradeops.model.response.OrderResponse;
import com.tradeops.repo.OrderRepo;
import com.tradeops.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trader/orders")
@RequiredArgsConstructor
public class TraderOrdersController {

    private final OrderService orderService;

    // FR-024: Владелец магазина видит ТОЛЬКО свои заказы (Tenant isolation)
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getTraderOrders(
            @RequestParam(name = "trader_id") Long traderId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(orderService.getAllOrders(traderId, pageable));
    }
}