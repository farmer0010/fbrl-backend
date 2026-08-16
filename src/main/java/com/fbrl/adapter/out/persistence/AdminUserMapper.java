package com.fbrl.adapter.out.persistence;

import com.fbrl.domain.model.AdminUser;
import org.springframework.stereotype.Component;

@Component
public class AdminUserMapper {

  public AdminUser toDomain(AdminUserJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return AdminUser.reconstruct(
        entity.getId(), entity.getUsername(), entity.getPasswordHash(), entity.getRole());
  }

  public AdminUserJpaEntity toEntity(AdminUser domain) {
    if (domain == null) {
      return null;
    }
    return new AdminUserJpaEntity(
        domain.getId(), domain.getUsername(), domain.getPasswordHash(), domain.getRole());
  }
}
