package com.tradeops.controller;

import com.tradeops.model.entity.Order;
import com.tradeops.model.request.CreateOrderRequest;
import com.tradeops.model.response.OrderResponse;
import com.tradeops.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/storefront/orders")
@RequiredArgsConstructor
public class StoreFrontOrdersController {

//    POST /api/v1/storefront/orders
//    GET  /api/v1/trader/orders
//    GET  /api/v1/admin/orders
//    PATCH /api/v1/admin/orders/{id}/status
//    POST /api/v1/admin/orders/{id}/assign
//    POST /api/v1/admin/orders/{id}/unassign


    private final OrderService orderService;

    // FR-023: Создание заказа с витрины магазина
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }
}