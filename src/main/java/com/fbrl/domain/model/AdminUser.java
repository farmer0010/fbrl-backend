package com.fbrl.domain.model;

import java.util.Objects;

public class AdminUser {
  private final Long id;
  private final String username;
  private final String passwordHash;
  private final AdminRole role;

  private AdminUser(Long id, String username, String passwordHash, AdminRole role) {
    this.id = id;
    this.username = Objects.requireNonNull(username, "사용자명은 필수입니다.");
    this.passwordHash = Objects.requireNonNull(passwordHash, "비밀번호 해시는 필수입니다.");
    this.role = Objects.requireNonNull(role, "역할은 필수입니다.");
  }

  public static AdminUser create(String username, String passwordHash) {
    return new AdminUser(null, username, passwordHash, AdminRole.ADMIN);
  }

  public static AdminUser reconstruct(
      Long id, String username, String passwordHash, AdminRole role) {
    return new AdminUser(id, username, passwordHash, role);
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public AdminRole getRole() {
    return role;
  }
}
