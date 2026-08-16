package com.fbrl.application.port.out;

import com.fbrl.domain.model.AdminUser;
import java.util.Optional;

public interface LoadAdminUserPort {
  Optional<AdminUser> loadByUsername(String username);
}
