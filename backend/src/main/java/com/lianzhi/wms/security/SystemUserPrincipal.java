package com.lianzhi.wms.security;

import com.lianzhi.wms.model.Permission;
import com.lianzhi.wms.model.Role;
import com.lianzhi.wms.model.SystemUser;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class SystemUserPrincipal implements UserDetails {
  private final Long userId;
  private final Long employeeId;
  private final String username;
  private final String roleCode;
  private final boolean enabled;
  private final Set<GrantedAuthority> authorities;

  private SystemUserPrincipal(Long userId,
                              Long employeeId,
                              String username,
                              String roleCode,
                              boolean enabled,
                              Set<GrantedAuthority> authorities) {
    this.userId = userId;
    this.employeeId = employeeId;
    this.username = username;
    this.roleCode = roleCode;
    this.enabled = enabled;
    this.authorities = authorities;
  }

  public static SystemUserPrincipal from(SystemUser user) {
    Role role = user.getRole();
    String roleCode = role == null || role.getCode() == null ? "STAFF" : role.getCode().trim().toUpperCase();
    Set<GrantedAuthority> authorities = new LinkedHashSet<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode));
    if (role != null && role.getPermissions() != null) {
      for (Permission permission : role.getPermissions()) {
        if (permission != null && permission.getCode() != null && !permission.getCode().isBlank()) {
          authorities.add(new SimpleGrantedAuthority(permission.getCode().trim().toUpperCase()));
        }
      }
    }
    Long employeeId = user.getEmployee() == null ? null : user.getEmployee().getId();
    return new SystemUserPrincipal(
        user.getId(),
        employeeId,
        user.getUsername(),
        roleCode,
        Boolean.TRUE.equals(user.getEnabled()),
        authorities
    );
  }

  @Override
  public String getPassword() {
    return "";
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }
}
