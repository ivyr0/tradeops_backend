package com.tradeops.mapper;

import com.tradeops.model.entity.InventoryItem;
import com.tradeops.model.response.InventoryItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ProductMapper.class})
public interface InventoryItemMapper {

    @Mapping(source = "product", target = "productResponse")
    InventoryItemResponse toInventoryItemResponse(InventoryItem inventoryItem);
}