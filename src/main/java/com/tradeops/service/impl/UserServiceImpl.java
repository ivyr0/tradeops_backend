package com.tradeops.service.impl;


import com.tradeops.exceptions.ResourceNotFoundException;
import com.tradeops.exceptions.UserAlreadyExistsException;
import com.tradeops.exceptions.UserNotFoundException;
import com.tradeops.model.entity.Role;
import com.tradeops.model.entity.UserEntity;
import com.tradeops.model.entity.UserRolePermission;
import com.tradeops.model.request.*;
import com.tradeops.model.response.*;
import com.tradeops.model.request.ChangePasswordRequest;
import com.tradeops.model.request.LoginRequest;
import com.tradeops.model.request.RefreshTokenRequest;
import com.tradeops.model.request.RegisterRequest;
import com.tradeops.model.response.JWTResponse;
import com.tradeops.model.response.LoginResponse;
import com.tradeops.model.response.RegistrationResponse;
import com.tradeops.repo.RoleRepo;
import com.tradeops.repo.UserEntityRepo;
import com.tradeops.service.JWTService;
import com.tradeops.service.CustomUserDetailsService;
import com.tradeops.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {


    private final UserEntityRepo userEntityRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JWTService jwtService;
    private final CustomUserDetailsService customUserDetailsService;


    @Override
    @Transactional
    public UserEntity register(RegisterRequest req, Role role) {
        if (!req.password().equals(req.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (userEntityRepo.existsByEmail(req.email())) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        UserEntity user = new UserEntity();
        user.setFullName(req.fullName());
        user.setEmail(req.email());
        user.setUsername(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setCreatedAt(LocalDateTime.now());

        user.setRoles(List.of(role));

        userEntityRepo.save(user);

        return user;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid username or password");
        }

        UserEntity user = userEntityRepo.findByUsername(request.username()).orElseThrow(()->new UserNotFoundException(request.username() + " not found"));
        List<String> scopes = new ArrayList<>(UserRolePermission.getScopesByRoleName(user.getRoles().getFirst().getName()));

        boolean isTrader = user.getRoles().stream().anyMatch(r -> r.getName().startsWith("ROLE_TRADER_"));
        if (isTrader && !user.isApproved()) {
            throw new BadCredentialsException("Your account is pending approval by SUPER_ADMIN.");
        }
        if(isTrader && user.isApproved()){
            return getLoginResponse(user, scopes);
        }

        if (user.isActive()) {
            return getLoginResponse(user, scopes);
        }

        userEntityRepo.save(user);

        return new LoginResponse(
                null,
                null,
                new User(
                        user.getId(),
                        user.getRoles().getFirst().getName(),
                        scopes)
        );
    }

    @NonNull
    private LoginResponse getLoginResponse(UserEntity user, List<String> scopes) {
        Authentication auth =
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        null,
                        user.getRoles().stream()
                                .map(r -> new SimpleGrantedAuthority(r.getName()))
                                .toList()
                );

        JWTResponse jwt = issueTokens(auth, scopes);


        return new LoginResponse(
                jwt.accessToken(),
                jwt.refreshToken(),
                new User(user.getId(),
                        user.getRoles().getFirst().getName(),
                        scopes)
        );
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        UserEntity user = getCurrentUser();

        if (!passwordEncoder.matches(req.oldPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid old password");
        }
        if (req.newPassword().length() < 8) {
            throw new IllegalArgumentException("Password too short");
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userEntityRepo.save(user);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(UpdateProfileRequest request) {
        UserEntity user = getCurrentUser();
        if(!Objects.equals(user.getFullName(), request.fullName())){
            user.setFullName(request.fullName());
            userEntityRepo.save(user);
        }

        return new ProfileResponse(user.getId(),user.getFullName(), user.getEmail());

    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(){
        UserEntity user = getCurrentUser();
        return new ProfileResponse(user.getId(), user.getFullName(), user.getEmail());
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String incomingRefreshToken = request.refreshToken();

        String username;
        try {
            username = jwtService.extractUserName(incomingRefreshToken);
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        UserEntity user = userEntityRepo.findByUsername(username).orElseThrow(()->new UserNotFoundException(username + " not found"));
        List<String> scopes = new ArrayList<>(UserRolePermission.getScopesByRoleName(user.getRoles().getFirst().getName()));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
        if (!jwtService.validateToken(incomingRefreshToken, userDetails)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        return getLoginResponse(user, scopes);
    }

    @Override
    public void logout(String token) {
        SecurityContextHolder.clearContext();

        log.info("User logged out successfully");
    }

    @Override
    public UserEntity getCurrentUser() {
        String username =
                Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        return userEntityRepo.findByUsername(username).orElseThrow(()->new UserNotFoundException("User not found with username: " + username));
    }

    private JWTResponse issueTokens(Authentication auth, List<String> scopes) {
        String access = jwtService.generateToken(auth, scopes);
        String refresh = jwtService.generateRefreshToken(auth);

        return new JWTResponse(access,refresh);
    }



}
