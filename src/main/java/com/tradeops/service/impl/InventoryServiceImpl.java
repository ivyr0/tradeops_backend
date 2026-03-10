package com.tradeops.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeops.annotation.Auditable;
import com.tradeops.exceptions.InsufficientStockException;
import com.tradeops.exceptions.ResourceNotFoundException;
import com.tradeops.mapper.InventoryItemMapper;
import com.tradeops.model.entity.InventoryItem;
import com.tradeops.model.entity.Product;
import com.tradeops.model.response.InventoryItemResponse;
import com.tradeops.repo.InventoryItemRepo;
import com.tradeops.repo.ProductRepo;
import com.tradeops.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryItemRepo inventoryItemRepo;
    private final ProductRepo productRepo;
    private final InventoryItemMapper inventoryItemMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryItemResponse> getInventoryList(Pageable pageable) {
        Page<InventoryItem> inventoryItems = inventoryItemRepo.findAll(pageable);

        return inventoryItems.map(inventoryItemMapper::toInventoryItemResponse);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Auditable(action = "INVENTORY_ADJUST", entityType = "INVENTORY_ITEM")
    public InventoryItemResponse adjustStock(Long productId, Integer newQtyOnHand) {
        if (newQtyOnHand < 0) {
            throw new IllegalArgumentException("Quantity on hand cannot be negative");
        }

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        InventoryItem item = inventoryItemRepo.findByProductId(productId)
                .orElseGet(() -> {
                    InventoryItem newItem = new InventoryItem();
                    newItem.setProduct(product);
                    newItem.setQtyOnHand(0);
                    newItem.setQtyReserved(0);
                    return newItem;
                });

        item.setQtyOnHand(newQtyOnHand);

        inventoryItemRepo.save(item);

        return inventoryItemMapper.toInventoryItemResponse(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Auditable(action = "STOCK_RESERVED", entityType = "INVENTORY_ITEM")
    public InventoryItemResponse reserveStock(Long productId, Integer qtyToReserve) {
        InventoryItem item = inventoryItemRepo.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        int available = item.getQtyOnHand() - item.getQtyReserved();
        if (qtyToReserve > available) {
            throw new InsufficientStockException("Not enough stock for product ID: " + productId);
        }

        item.setQtyReserved(item.getQtyReserved() + qtyToReserve);
        inventoryItemRepo.save(item);

        return inventoryItemMapper.toInventoryItemResponse(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Auditable(action = "STOCK_RELEASED", entityType = "INVENTORY_ITEM")
    public InventoryItemResponse releaseStock(Long productId, Integer qtyToRelease) {
        InventoryItem item = inventoryItemRepo.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        int newReserved = Math.max(0, item.getQtyReserved() - qtyToRelease);
        item.setQtyReserved(newReserved);
        inventoryItemRepo.save(item);

        return inventoryItemMapper.toInventoryItemResponse(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Auditable(action = "STOCK_FULFILLED", entityType = "INVENTORY_ITEM")
    public void fulfillStock(Long productId, Integer qtyToFulfill) {
        InventoryItem item = inventoryItemRepo.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        item.setQtyOnHand(item.getQtyOnHand() - qtyToFulfill);
        item.setQtyReserved(item.getQtyReserved() - qtyToFulfill);

        inventoryItemRepo.save(item);
    }
}