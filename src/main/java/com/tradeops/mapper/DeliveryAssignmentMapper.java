package com.tradeops.mapper;

import com.tradeops.model.entity.DeliveryAssignment;
import com.tradeops.model.response.DeliveryAssignmentResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {OrderMapper.class, CourierUserMapper.class})
public interface DeliveryAssignmentMapper {

    DeliveryAssignmentResponse toDeliveryAssignmentResponse(DeliveryAssignment assignment);

    List<DeliveryAssignmentResponse> toDeliveryAssignmentResponseList(List<DeliveryAssignment> assignments);
}