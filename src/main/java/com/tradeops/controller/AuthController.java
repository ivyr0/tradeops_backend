package com.tradeops.controller;

import com.tradeops.model.request.ChangePasswordRequest;
import com.tradeops.model.request.LoginRequest;
import com.tradeops.model.request.RefreshTokenRequest;
import com.tradeops.model.request.UpdateProfileRequest;
import com.tradeops.model.response.LoginResponse;
import com.tradeops.model.response.ProfileResponse;
import com.tradeops.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.config.RepositoryNameSpaceHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse httpServletResponse){
        return ResponseEntity.ok(userService.login(loginRequest, httpServletResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@RequestBody RefreshTokenRequest rtr){
        return ResponseEntity.ok(userService.refreshToken(rtr));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            userService.logout(token);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> me(){
        return ResponseEntity.ok(userService.getProfile());
    }

    @PostMapping("/update-profile")
    public ResponseEntity<ProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request){
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request){
        userService.changePassword(request);
        return ResponseEntity.ok().build();
    }
}
