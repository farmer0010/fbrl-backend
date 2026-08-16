package com.fbrl.adapter.out.security;

import com.fbrl.application.port.out.LoadAdminUserPort;
import com.fbrl.domain.model.AdminUser;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class AdminUserDetailsService implements UserDetailsService {

  private final LoadAdminUserPort loadAdminUserPort;

  public AdminUserDetailsService(LoadAdminUserPort loadAdminUserPort) {
    this.loadAdminUserPort = loadAdminUserPort;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    AdminUser adminUser =
        loadAdminUserPort
            .loadByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));

    return User.withUsername(adminUser.getUsername())
        .password(adminUser.getPasswordHash())
        .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + adminUser.getRole().name())))
        .build();
  }
}
