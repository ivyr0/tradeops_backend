package com.tradeops.service;

import com.tradeops.other.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JWTService {

  @Value("${tradeops.app.secret}")
  private String secretKey;

  private SecretKey getKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public String generateToken(Authentication authentication, List<String> scopes) {
    String username = authentication.getName();
    List<String> roles = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toList());

    Date now = new Date();
    Date expiry = new Date(now.getTime() + SecurityConstants.JWT_EXPIRATION_TIME);

    return Jwts.builder()
        .subject(username)
        .claim("roles", roles)
        .claim("scopes", scopes) // Add this claim
        .issuedAt(now)
        .expiration(expiry)
        .signWith(getKey())
        .compact();
  }

  public String generateRefreshToken(Authentication authentication) {
    String username = authentication.getName();
    Date currentDate = new Date();
    Date expirationDate = new Date(currentDate.getTime() + SecurityConstants.JWT_REFRESH_EXPIRATION_TIME);

    return Jwts.builder()
        .subject(username)
        .issuedAt(currentDate)
        .expiration(expirationDate)
        .signWith(getKey())
        .compact();
  }
  // telusko method
  // public String generateToken(Authentication authentication) {
  // Map<String, Object> claims = new HashMap<>();
  //
  // String username = authentication.getName();
  // List<String> roles = authentication.getAuthorities().stream()
  // .map(GrantedAuthority::getAuthority).toList();
  //
  // return Jwts.builder()
  // .claims()
  // .add(claims)
  // .subject(username)
  // .issuedAt(new Date(System.currentTimeMillis()))
  // .expiration(new Date(System.currentTimeMillis() +
  // SecurityConstants.JWT_REFRESH_EXPIRATION_TIME))
  // .and()
  // .signWith(getKey())
  // .compact();
  //
  // }

  public String extractUserName(String token) {
    // extract the username from jwt token
    return extractClaim(token, Claims::getSubject);
  }

  private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
    final Claims claims = extractAllClaims(token);
    return claimResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith(getKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public boolean validateToken(String token, UserDetails userDetails) {
    final String userName = extractUserName(token);
    return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
  }

  public boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

}
