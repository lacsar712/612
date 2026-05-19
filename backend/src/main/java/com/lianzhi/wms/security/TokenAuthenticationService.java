package com.lianzhi.wms.security;

import com.lianzhi.wms.model.SystemUser;
import com.lianzhi.wms.repository.SystemUserRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class TokenAuthenticationService {
  private final ConcurrentMap<String, Long> activeTokens = new ConcurrentHashMap<>();
  private final SystemUserRepository systemUserRepository;

  public TokenAuthenticationService(SystemUserRepository systemUserRepository) {
    this.systemUserRepository = systemUserRepository;
  }

  public String issueToken(SystemUser user) {
    String token = "WMS-" + UUID.randomUUID().toString().replace("-", "");
    activeTokens.put(token, user.getId());
    return token;
  }

  public Optional<SystemUserPrincipal> authenticate(String token) {
    Long userId = activeTokens.get(token);
    if (userId == null) {
      return Optional.empty();
    }
    return systemUserRepository.findById(userId)
        .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
        .map(SystemUserPrincipal::from);
  }
}
