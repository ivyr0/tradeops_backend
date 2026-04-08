package com.tradeops.service;

import com.tradeops.model.entity.Role;
import com.tradeops.model.entity.UserEntity;
import com.tradeops.model.request.*;
import com.tradeops.model.request.ChangePasswordRequest;
import com.tradeops.model.request.LoginRequest;
import com.tradeops.model.request.RefreshTokenRequest;
import com.tradeops.model.request.RegisterRequest;
import com.tradeops.model.response.LoginResponse;
import com.tradeops.model.response.ProfileResponse;
import com.tradeops.model.response.RegistrationResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface UserService {
//    RegistrationResponse registerTrader(RegisterRequest request);
//    RegistrationResponse registerCustomer(RegisterRequest request);
    UserEntity register(RegisterRequest request, Role role);
    LoginResponse login(LoginRequest loginRequest, HttpServletResponse response);
    void changePassword(ChangePasswordRequest changePasswordRequest);
    UserEntity getCurrentUser();
    LoginResponse refreshToken(RefreshTokenRequest refreshTokenRequest);
    void logout(String token);
    ProfileResponse getProfile();
    ProfileResponse updateProfile(UpdateProfileRequest request);
}
