package com.tradeops.service;

import com.tradeops.model.request.CourierCreateRequest;
import com.tradeops.model.request.CourierUpdateRequest;
import com.tradeops.model.response.CourierUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourierAdminService {
    CourierUserResponse createCourier(CourierCreateRequest request);
    Page<CourierUserResponse> getAllCouriers(Pageable pageable);
    CourierUserResponse getCourierById(Long id);
    CourierUserResponse updateCourier(Long id, CourierUpdateRequest request);
    void toggleCourierStatus(Long id, boolean isActive);
    Void deleteCourier(Long id);
}
