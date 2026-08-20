package com.fbrl.application.port.out;

import com.fbrl.domain.model.AdminUser;
import java.util.Optional;

public interface TokenPort {
  String issueToken(AdminUser adminUser);

  Optional<String> validateToken(String token);

  Optional<String> extractRole(String token);
}
