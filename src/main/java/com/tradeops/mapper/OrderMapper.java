package com.tradeops.mapper;

import com.tradeops.model.entity.Order;
import com.tradeops.model.entity.OrderLine;
import com.tradeops.model.response.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "trader.id", target = "traderId")
    @Mapping(source = "customerLink.id", target = "customerLinkId")
    @Mapping(source = "orderLines", target = "orderLinesId")
    OrderResponse toOrderResponse(Order order);


    default Long mapOrderLineToId(OrderLine orderLine) {
        if (orderLine == null) {
            return null;
        }
        return orderLine.getId();
    }
}
