package com.tradeops.service.impl;

import com.tradeops.annotation.Auditable;
import com.tradeops.exceptions.ResourceNotFoundException;
import com.tradeops.exceptions.UserAlreadyExistsException;
import com.tradeops.mapper.CourierUserMapper;
import com.tradeops.model.entity.CourierUser;
import com.tradeops.model.entity.Role;
import com.tradeops.model.entity.UserEntity;
import com.tradeops.model.request.CourierCreateRequest;
import com.tradeops.model.request.CourierUpdateRequest;
import com.tradeops.model.response.CourierUserResponse;
import com.tradeops.repo.CourierUserRepo;
import com.tradeops.repo.RoleRepo;
import com.tradeops.repo.UserEntityRepo;
import com.tradeops.service.CourierAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourierAdminServiceImpl implements CourierAdminService {

    private final CourierUserRepo courierUserRepo;
    private final UserEntityRepo userEntityRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final CourierUserMapper courierUserMapper;

    @Override
    @Transactional
    @Auditable(action = "COURIER_CREATED", entityType = "COURIER")
    public CourierUserResponse createCourier(CourierCreateRequest request) {
        if (userEntityRepo.existsByUsername(request.phone())) {
            throw new UserAlreadyExistsException("Phone number already registered as user");
        }
        if (courierUserRepo.findByPhone(request.phone()).isPresent()) {
            throw new UserAlreadyExistsException("Phone number already registered to a courier");
        }

        Role courierRole = roleRepo.findByName("ROLE_COURIER")
                .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_COURIER not found"));

        UserEntity user = new UserEntity();
        user.setUsername(request.phone());
        // Using phone as email placeholder to satisfy any non-null email constraints, or just phone
        user.setEmail(request.phone() + "@courier.tradeops.kg"); 
        user.setFullName(request.name());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(List.of(courierRole));
        user.setCreatedAt(LocalDateTime.now());
        user.setApproved(true);
        userEntityRepo.save(user);

        CourierUser courier = new CourierUser();
        courier.setName(request.name());
        courier.setPhone(request.phone());
        courier.setRoles(List.of(courierRole));
        courier.setIsActive(true);
        courier = courierUserRepo.save(courier);

        return courierUserMapper.toCourierUserResponse(courier);
    }

    @Override
    public Page<CourierUserResponse> getAllCouriers(Pageable pageable) {
        return courierUserRepo.findAll(pageable)
                .map(courierUserMapper::toCourierUserResponse);
    }

    @Override
    public CourierUserResponse getCourierById(Long id) {
        return courierUserRepo.findById(id)
                .map(courierUserMapper::toCourierUserResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Courier not found"));
    }

    @Override
    @Transactional
    @Auditable(action = "COURIER_UPDATED", entityType = "COURIER")
    public CourierUserResponse updateCourier(Long id, CourierUpdateRequest request) {
        CourierUser courier = courierUserRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Courier not found"));

        if (!courier.getPhone().equals(request.phone()) && courierUserRepo.findByPhone(request.phone()).isPresent()) {
            throw new UserAlreadyExistsException("Phone number already taken by another courier");
        }

        courier.setName(request.name());
        courier.setPhone(request.phone());
        
        if (request.isActive() != null) {
            courier.setIsActive(request.isActive());
        }

        courier = courierUserRepo.save(courier);

        // Update corresponding UserEntity if phone/name changed
        userEntityRepo.findByUsername(courier.getPhone()).ifPresent(user -> {
            user.setFullName(request.name());
            user.setUsername(request.phone());
            userEntityRepo.save(user);
        });

        return courierUserMapper.toCourierUserResponse(courier);
    }

    @Override
    @Transactional
    @Auditable(action = "COURIER_STATUS_TOGGLED", entityType = "COURIER")
    public void toggleCourierStatus(Long id, boolean isActive) {
        CourierUser courier = courierUserRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Courier not found"));
        courier.setIsActive(isActive);
        courierUserRepo.save(courier);

        userEntityRepo.findByUsername(courier.getPhone()).ifPresent(user -> {
            user.setActive(isActive);
            userEntityRepo.save(user);
        });
    }

    @Override
    @Transactional
    @Auditable(action = "COURIER_DELETED", entityType = "COURIER")
    public Void deleteCourier(Long id) {
        CourierUser courier = courierUserRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Courier not found"));

        userEntityRepo.findByUsername(courier.getPhone()).ifPresent(userEntityRepo::delete);
        
        courierUserRepo.delete(courier);
        return null;
    }
}
