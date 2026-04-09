package com.tradeops.service.impl;

import com.tradeops.exceptions.InvalidPaymentMethodException;
import com.tradeops.exceptions.InvalidStatusTransitionException;
import com.tradeops.exceptions.ResourceNotFoundException;
import com.tradeops.mapper.OrderMapper;
import com.tradeops.model.entity.*;
import com.tradeops.model.request.CreateOrderRequest;
import com.tradeops.model.request.OrderLineRequest;
import com.tradeops.model.response.OrderResponse;
import com.tradeops.model.response.OrdersCountResponse;
import com.tradeops.repo.CustomerLinkRepo;
import com.tradeops.repo.OrderRepo;
import com.tradeops.repo.ProductRepo;
import com.tradeops.repo.TraderRepo;
import com.tradeops.service.InventoryService;
import com.tradeops.service.OrderService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import com.tradeops.annotation.Auditable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;


import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepo orderRepo;
    private final TraderRepo traderRepo;
    private final ProductRepo productRepo;
    private final InventoryService inventoryService;
    private final CustomerLinkRepo customerLinkRepo;

    private final OrderMapper orderMapper;

    @Override
    @Transactional(readOnly = true)
    public OrdersCountResponse getOrdersCount() {
        return new OrdersCountResponse(orderRepo.count());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(CreateOrderRequest request) {

        if (!"COD".equalsIgnoreCase(request.paymentMethod())) {
            throw new InvalidPaymentMethodException("Only COD (Cash On Delivery) is supported");
        }

        Trader trader = traderRepo.findById(request.traderId())
                .orElseThrow(() -> new ResourceNotFoundException("Trader not found"));

        String contactHash = hashContact(request.customerPhone());
        CustomerLink customerLink = customerLinkRepo.findByContactHash(contactHash)
                .orElseGet(() -> {
                    CustomerLink newLink = new CustomerLink();
                    newLink.setTrader(trader);
                    newLink.setContactHash(contactHash);
                    newLink.setCustomerExternalId(UUID.randomUUID().toString());
                    return customerLinkRepo.save(newLink);
                });

        Order order = new Order();
        order.setTrader(trader);
        order.setCustomerLink(customerLink);
        order.setDeliveryAddress(request.deliveryAddress());
        order.setStatus(OrderStatus.NEW);

        order.setOrderNumber("KG-" + System.currentTimeMillis());

        BigDecimal totals = BigDecimal.ZERO;
        List<OrderLine> orderLines = new ArrayList<>();

        for (OrderLineRequest lineReq : request.lines()) {
            Product product = productRepo.findById(lineReq.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + lineReq.productId()));

            inventoryService.reserveStock(product.getId(), lineReq.quantity());

            OrderLine orderLine = new OrderLine();
            orderLine.setOrder(order);
            orderLine.setProduct(product);
            orderLine.setQty(lineReq.quantity());
            orderLine.setUnitPrice(product.getBasePrice());

            BigDecimal lineTotal = product.getBasePrice().multiply(BigDecimal.valueOf(lineReq.quantity()));
            orderLine.setLineTotal(lineTotal);

            orderLines.add(orderLine);
            totals = totals.add(lineTotal);
        }

        order.setTotals(totals);
        order.setOrderLines(orderLines);
        Order savedOrder = orderRepo.save(order);

        return orderMapper.toOrderResponse(savedOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Auditable(action = "ORDER_STATUS_CHANGED", entityType = "ORDER")
    public OrderResponse changeOrderStatus(@NotNull Long orderId, @NotNull OrderStatus newStatus) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        getOrderStatus(newStatus, order);

        if (newStatus == OrderStatus.CANCELLED) {
            for (OrderLine line : order.getOrderLines()) {
                inventoryService.releaseStock(line.getProduct().getId(), line.getQty());
            }
        }

        order.setStatus(newStatus);
        Order savedOrder = orderRepo.save(order);

        return orderMapper.toOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        Page<Order> ordersPage = orderRepo.findAll(pageable);

        return ordersPage.map(orderMapper::toOrderResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<OrderResponse> getAllOrders(Long traderId, Pageable pageable) {
        Page<Order> ordersPage = orderRepo.findByTraderId(traderId, pageable);

        return ordersPage.map(orderMapper::toOrderResponse);
    }

    private static @NonNull OrderStatus getOrderStatus(OrderStatus newStatus, Order order) {
        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == OrderStatus.COMPLETED || currentStatus == OrderStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Cannot change status of closed order");
        }

        boolean isValidStatusTransition = switch (currentStatus) {
            case NEW -> newStatus == OrderStatus.ASSIGNED || newStatus == OrderStatus.CANCELLED;
            case ASSIGNED -> newStatus == OrderStatus.ON_PROGRESS || newStatus == OrderStatus.CANCELLED;
            case ON_PROGRESS -> newStatus == OrderStatus.COMPLETED || newStatus == OrderStatus.CANCELLED;
            default -> false;
        };

        if (!isValidStatusTransition) {
            throw new InvalidStatusTransitionException("Invalid transition from " + currentStatus + " to " + newStatus);
        }
        return currentStatus;
    }

    private String hashContact(String contactInfo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(contactInfo.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash contact info", e);
        }
    }
}