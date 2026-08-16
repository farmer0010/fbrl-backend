package com.fbrl.application.port.out;

import com.fbrl.domain.model.AdminUser;

public interface SaveAdminUserPort {
  AdminUser save(AdminUser adminUser);
}
