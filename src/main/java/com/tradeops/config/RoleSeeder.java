package com.tradeops.config;

import com.tradeops.exceptions.ResourceNotFoundException;
import com.tradeops.model.entity.Role;
import com.tradeops.model.entity.UserEntity;
import com.tradeops.repo.RoleRepo;
import com.tradeops.repo.UserEntityRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.management.relation.RoleNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class RoleSeeder implements CommandLineRunner {
  private final PasswordEncoder passwordEncoder;
  private final UserEntityRepo userRepo;
  private final RoleRepo roleRepo;

  @Value("${tradeops.app.admin.username}")
  private String ADMIN_USERNAME;

  @Value("${tradeops.app.admin.password}")
  private String ADMIN_PASSWORD;

  public RoleSeeder(RoleRepo roleRepo,
                    UserEntityRepo userRepo,
                    PasswordEncoder passwordEncoder){
    this.roleRepo = roleRepo;
    this.userRepo = userRepo;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    Role adminRole = roleRepo.findByName("ROLE_SUPER_ADMIN").orElseThrow(()->new ResourceNotFoundException("ROLE_ADMIN not found"));

    userRepo.findByUsername(ADMIN_USERNAME).ifPresentOrElse(
            user -> {
              boolean hasAdminRole = user.getRoles().stream()
                      .anyMatch(r -> "ROLE_SUPER_ADMIN".equals(r.getName()));

              if (!hasAdminRole) {
                user.getRoles().add(adminRole);
                userRepo.save(user);
              }
            },
            () -> {
              var user = new UserEntity();
              user.setUsername(ADMIN_USERNAME);
              user.setEmail("admin@tradeops.com");
              user.setFullName("Super Administrator");
              user.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
              user.setRoles(new ArrayList<>(Collections.singletonList(adminRole)));
              user.setCreatedAt(LocalDateTime.now());
              user.setActive(true);
              userRepo.save(user);
              System.out.println("✅ Super Admin created: " + ADMIN_USERNAME);

            }
    );
  }
}