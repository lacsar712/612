package com.lianzhi.wms.controller;

import com.lianzhi.wms.model.SystemUser;
import com.lianzhi.wms.repository.SystemUserRepository;
import com.lianzhi.wms.security.TokenAuthenticationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {
  private final SystemUserRepository systemUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenAuthenticationService tokenAuthenticationService;

  public AuthController(SystemUserRepository systemUserRepository,
                        PasswordEncoder passwordEncoder,
                        TokenAuthenticationService tokenAuthenticationService) {
    this.systemUserRepository = systemUserRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenAuthenticationService = tokenAuthenticationService;
  }

  @PostMapping("/login")
  @ResponseStatus(HttpStatus.OK)
  public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
    SystemUser user = systemUserRepository.findByUsername(request.username())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    if (!Boolean.TRUE.equals(user.getEnabled()) || user.getPasswordHash() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
    return Map.of(
        "token", tokenAuthenticationService.issueToken(user),
        "user", user
    );
  }

  public record LoginRequest(
      @NotBlank(message = "用户名不能为空") String username,
      @NotBlank(message = "密码不能为空") String password
  ) {}
}
