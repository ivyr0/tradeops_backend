package com.tradeops.service;

import com.tradeops.exceptions.ResourceNotFoundException;
import com.tradeops.model.entity.InventoryItem;
import com.tradeops.model.entity.Product;
import com.tradeops.model.response.InventoryItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {
    Page<InventoryItemResponse> getInventoryList(Pageable pageable);

    InventoryItemResponse adjustStock(Long productId, Integer newQtyOnHand);

    InventoryItemResponse reserveStock(Long productId, Integer qtyToReserve);

    InventoryItemResponse releaseStock(Long productId, Integer qtyToRelease);

    void fulfillStock(Long productId, Integer qtyToFulfill);

}
