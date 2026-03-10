package com.tradeops.model.response;

public record InventoryItemResponse(
        Long id,
        ProductResponse productResponse,
        Integer qtyOnHand,
        Integer qtyReserved,
        Long warehouseId,
        Integer reorderLevel
) {}
