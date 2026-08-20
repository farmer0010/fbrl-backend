package com.fbrl.adapter.out.security;

import com.fbrl.application.port.out.TokenPort;
import com.fbrl.domain.model.AdminUser;
import com.fbrl.global.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenAdapter implements TokenPort {

  private final SecretKey signingKey;
  private final long expirationSeconds;

  public JwtTokenAdapter(JwtProperties jwtProperties) {
    this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    this.expirationSeconds = jwtProperties.expirationSeconds();
  }

  @Override
  public String issueToken(AdminUser adminUser) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(adminUser.getUsername())
        .claim("role", adminUser.getRole().name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(expirationSeconds)))
        .signWith(signingKey)
        .compact();
  }

  @Override
  public Optional<String> validateToken(String token) {
    try {
      String username =
          Jwts.parser()
              .verifyWith(signingKey)
              .build()
              .parseSignedClaims(token)
              .getPayload()
              .getSubject();
      return Optional.of(username);
    } catch (JwtException | IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<String> extractRole(String token) {
    try {
      String role =
          Jwts.parser()
              .verifyWith(signingKey)
              .build()
              .parseSignedClaims(token)
              .getPayload()
              .get("role", String.class);
      return Optional.ofNullable(role);
    } catch (JwtException | IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
