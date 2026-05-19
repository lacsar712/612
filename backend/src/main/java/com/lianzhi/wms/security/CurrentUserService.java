package com.lianzhi.wms.security;

import java.util.Arrays;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service("currentUser")
public class CurrentUserService {
  public SystemUserPrincipal requirePrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken
        || !(authentication.getPrincipal() instanceof SystemUserPrincipal principal)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或登录已失效");
    }
    return principal;
  }

  public Long requireEmployeeId() {
    Long employeeId = requirePrincipal().getEmployeeId();
    if (employeeId == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前用户未绑定员工信息");
    }
    return employeeId;
  }

  public boolean hasRole(String roleCode) {
    return roleCode != null && roleCode.equalsIgnoreCase(requirePrincipal().getRoleCode());
  }

  public boolean hasAnyRole(String... roleCodes) {
    return Arrays.stream(roleCodes).anyMatch(this::hasRole);
  }

  public boolean isAdmin() {
    return hasRole("ADMIN");
  }

  public boolean isKeeper() {
    return hasRole("KEEPER");
  }

  public boolean isStaff() {
    return hasRole("STAFF");
  }

  public boolean canViewAllEmployees() {
    return isAdmin() || isKeeper();
  }

  public boolean canViewEmployee(Long employeeId) {
    if (canViewAllEmployees()) {
      return true;
    }
    return employeeId != null && Objects.equals(requireEmployeeId(), employeeId);
  }

  public void ensureOwnEmployee(Long employeeId) {
    if (!Objects.equals(requireEmployeeId(), employeeId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "普通员工只能操作自己的数据");
    }
  }
}
