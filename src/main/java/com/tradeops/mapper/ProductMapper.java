package com.tradeops.mapper;

import com.tradeops.model.entity.Product;
import com.tradeops.model.response.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(source = "category.id", target = "categoryId")

    @Mapping(target = "availableQty", ignore = true)
    ProductResponse toProductResponse(Product product);
}