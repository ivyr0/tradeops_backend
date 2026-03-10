package com.tradeops.service;

import com.tradeops.model.entity.CourierUser;
import com.tradeops.model.entity.DeliveryAssignment;
import com.tradeops.model.response.DeliveryAssignmentResponse;

import java.util.List;

public interface CourierService {
    List<DeliveryAssignmentResponse> getActiveAssignments();
    DeliveryAssignmentResponse acceptAssignment(Long assignmentId);
    DeliveryAssignmentResponse completeAssignment(Long assignmentId);
}
