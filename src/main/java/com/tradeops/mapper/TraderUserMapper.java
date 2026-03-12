package com.tradeops.mapper;

import com.tradeops.model.entity.TraderUser;
import com.tradeops.model.response.TraderUserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TraderUserMapper {
    TraderUserResponse toTraderUserResponse(TraderUser traderUser);}
