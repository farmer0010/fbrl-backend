package com.fbrl.adapter.in.runner;

import com.fbrl.application.port.out.LoadAdminUserPort;
import com.fbrl.application.port.out.SaveAdminUserPort;
import com.fbrl.domain.model.AdminUser;
import com.fbrl.global.config.AdminInitialCredentialsProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserSeeder implements ApplicationRunner {

  private final AdminInitialCredentialsProperties adminInitialCredentialsProperties;
  private final LoadAdminUserPort loadAdminUserPort;
  private final SaveAdminUserPort saveAdminUserPort;
  private final PasswordEncoder passwordEncoder;

  public AdminUserSeeder(
      AdminInitialCredentialsProperties adminInitialCredentialsProperties,
      LoadAdminUserPort loadAdminUserPort,
      SaveAdminUserPort saveAdminUserPort,
      PasswordEncoder passwordEncoder) {
    this.adminInitialCredentialsProperties = adminInitialCredentialsProperties;
    this.loadAdminUserPort = loadAdminUserPort;
    this.saveAdminUserPort = saveAdminUserPort;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(ApplicationArguments args) {
    String username = adminInitialCredentialsProperties.username();
    String password = adminInitialCredentialsProperties.password();

    if (username == null || username.isBlank() || password == null || password.isBlank()) {
      return;
    }

    if (loadAdminUserPort.loadByUsername(username).isPresent()) {
      return;
    }

    saveAdminUserPort.save(AdminUser.create(username, passwordEncoder.encode(password)));
  }
}
