package com.tradeops.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JWTFilter jwtFilter;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers("/api/v1/catalog/**").permitAll()
                    .requestMatchers("/api/v1/storefront/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui/index.html", "/swagger-ui.html").permitAll()

                    .requestMatchers("/api/v1/admin/catalog/**")
                    .hasAnyAuthority("SUPER_ADMIN", "CATALOG_MANAGER", "ROLE_SUPER_ADMIN", "ROLE_CATALOG_MANAGER")

                    .requestMatchers("/api/v1/admin/inventory/**")
                    .hasAnyAuthority("SUPER_ADMIN", "CATALOG_MANAGER", "WAREHOUSE_OPS", "ROLE_SUPER_ADMIN", "ROLE_CATALOG_MANAGER", "ROLE_WAREHOUSE_OPS")

                    .requestMatchers("/api/v1/admin/orders/**")
                    .hasAnyAuthority("SUPER_ADMIN", "WAREHOUSE_OPS", "DISPATCHER", "ROLE_SUPER_ADMIN", "ROLE_WAREHOUSE_OPS", "ROLE_DISPATCHER")

                    .requestMatchers("/api/v1/trader/**")
                    .hasAnyAuthority("TRADER_ADMIN", "TRADER_STAFF", "ROLE_TRADER_ADMIN", "ROLE_TRADER_STAFF")

                    .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOriginPatterns(List.of("*"));

    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control"));

    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }
}