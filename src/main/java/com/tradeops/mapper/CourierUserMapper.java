package com.tradeops.mapper;

import com.tradeops.model.entity.CourierUser;
import com.tradeops.model.response.CourierUserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CourierUserMapper {
    CourierUserResponse toCourierUserResponse(CourierUser courier);
}