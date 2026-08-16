package com.fbrl.adapter.out.persistence;

import com.fbrl.application.port.out.LoadAdminUserPort;
import com.fbrl.application.port.out.SaveAdminUserPort;
import com.fbrl.domain.exception.AdminUserPersistenceException;
import com.fbrl.domain.exception.DuplicateAdminUsernameException;
import com.fbrl.domain.model.AdminUser;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class AdminUserPersistenceAdapter implements LoadAdminUserPort, SaveAdminUserPort {

  private final AdminUserJpaRepository adminUserJpaRepository;
  private final AdminUserMapper adminUserMapper;

  public AdminUserPersistenceAdapter(
      AdminUserJpaRepository adminUserJpaRepository, AdminUserMapper adminUserMapper) {
    this.adminUserJpaRepository = adminUserJpaRepository;
    this.adminUserMapper = adminUserMapper;
  }

  @Override
  public Optional<AdminUser> loadByUsername(String username) {
    try {
      return adminUserJpaRepository.findByUsername(username).map(adminUserMapper::toDomain);
    } catch (DataAccessException e) {
      throw new AdminUserPersistenceException("관리자 계정 조회 중 인프라 예외가 발생했습니다.", e);
    }
  }

  @Override
  public AdminUser save(AdminUser adminUser) {
    try {
      AdminUserJpaEntity entity = adminUserMapper.toEntity(adminUser);
      AdminUserJpaEntity savedEntity = adminUserJpaRepository.save(entity);
      return adminUserMapper.toDomain(savedEntity);
    } catch (DataIntegrityViolationException e) {
      throw new DuplicateAdminUsernameException(
          "이미 존재하는 관리자 계정입니다. username: " + adminUser.getUsername());
    } catch (DataAccessException e) {
      throw new AdminUserPersistenceException("관리자 계정 저장 중 인프라 예외가 발생했습니다.", e);
    }
  }

  public void deleteAllInBatch() {
    adminUserJpaRepository.deleteAllInBatch();
  }
}
