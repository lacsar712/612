package com.lianzhi.wms.controller;

import com.lianzhi.wms.model.Employee;
import com.lianzhi.wms.model.Role;
import com.lianzhi.wms.model.SystemUser;
import com.lianzhi.wms.repository.EmployeeRepository;
import com.lianzhi.wms.repository.RoleRepository;
import com.lianzhi.wms.repository.SystemUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class SystemUserController {
  private final SystemUserRepository repository;
  private final RoleRepository roleRepository;
  private final EmployeeRepository employeeRepository;
  private final PasswordEncoder passwordEncoder;

  public SystemUserController(SystemUserRepository repository,
                              RoleRepository roleRepository,
                              EmployeeRepository employeeRepository,
                              PasswordEncoder passwordEncoder) {
    this.repository = repository;
    this.roleRepository = roleRepository;
    this.employeeRepository = employeeRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @GetMapping
  public List<SystemUser> list() {
    return repository.findAll();
  }

  @GetMapping("/{id}")
  public SystemUser get(@PathVariable("id") Long id) {
    return repository.findById(id).orElseThrow();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SystemUser create(@Valid @RequestBody SystemUserRequest request) {
    if (repository.existsByUsername(request.username())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名已存在");
    }
    if (request.password() == null || request.password().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新增用户必须设置密码");
    }
    SystemUser systemUser = new SystemUser();
    applyRequest(systemUser, request, true);
    return repository.save(systemUser);
  }

  @PutMapping("/{id}")
  public SystemUser update(@PathVariable("id") Long id, @Valid @RequestBody SystemUserRequest request) {
    SystemUser systemUser = repository.findById(id).orElseThrow();
    if (!systemUser.getUsername().equals(request.username()) && repository.existsByUsername(request.username())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名已存在");
    }
    applyRequest(systemUser, request, false);
    systemUser.setId(id);
    return repository.save(systemUser);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable("id") Long id) {
    repository.deleteById(id);
  }

  private void applyRequest(SystemUser systemUser, SystemUserRequest request, boolean create) {
    Long roleId = request.role().id();
    Long employeeId = request.employee().id();
    Role role = roleRepository.findById(roleId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色不存在"));
    Employee employee = employeeRepository.findById(employeeId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "员工不存在"));
    repository.findByEmployeeId(employeeId).ifPresent(existing -> {
      if (systemUser.getId() == null || !existing.getId().equals(systemUser.getId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该员工已绑定其他系统用户");
      }
    });

    systemUser.setUsername(request.username());
    systemUser.setEnabled(request.enabled());
    systemUser.setRole(role);
    systemUser.setEmployee(employee);

    if (request.password() != null && !request.password().isBlank()) {
      validatePassword(request.password());
      systemUser.setPasswordHash(passwordEncoder.encode(request.password()));
    } else if (create) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新增用户必须设置密码");
    }
  }

  private void validatePassword(String password) {
    if (password.length() < 6) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码长度不能少于6位");
    }
    boolean hasLetter = password.chars().anyMatch(Character::isLetter);
    boolean hasDigit = password.chars().anyMatch(Character::isDigit);
    if (!hasLetter || !hasDigit) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码需同时包含字母和数字");
    }
  }

  public record SystemUserRequest(
      @NotBlank(message = "用户名不能为空") String username,
      String password,
      @NotNull(message = "启用状态不能为空") Boolean enabled,
      @NotNull(message = "角色不能为空") IdRef role,
      @NotNull(message = "员工不能为空") IdRef employee
  ) {}

  public record IdRef(@NotNull(message = "关联ID不能为空") Long id) {}
}
