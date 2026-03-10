package com.tradeops.controller;

import com.tradeops.model.entity.Order;
import com.tradeops.model.entity.OrderStatus;
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
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrdersController {

    private final OrderService orderService;

    // FR-025: Просмотр всех заказов компании (с фильтрацией)
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrders(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    // FR-026, FR-027: Смена статуса заказа
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> changeStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {

        return ResponseEntity.ok(orderService.changeOrderStatus(id, status));
    }
}