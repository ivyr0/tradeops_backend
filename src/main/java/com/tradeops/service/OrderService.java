package com.tradeops.service;

import com.tradeops.model.entity.Order;
import com.tradeops.model.entity.OrderStatus;
import com.tradeops.model.request.CreateOrderRequest;
import com.tradeops.model.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.awt.print.Pageable;
import java.util.List;

public interface OrderService {

    // FR-023: Создание заказа с витрины (Резервирует склад, считает сумму, ставит
    // статус NEW)
    OrderResponse createOrder(CreateOrderRequest request);

    // FR-026, FR-027: Перевод заказа по статусам (NEW -> ASSIGNED -> ON_PROGRESS ->
    // COMPLETED)
    OrderResponse changeOrderStatus(Long orderId, OrderStatus newStatus);

    @Transactional(readOnly = true)
    Page<OrderResponse> getAllOrders(org.springframework.data.domain.Pageable pageable);

    @Transactional(readOnly = true)
    Page<OrderResponse> getAllOrders(Long traderId, org.springframework.data.domain.Pageable pageable);
}