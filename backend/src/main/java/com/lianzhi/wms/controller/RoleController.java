package com.lianzhi.wms.controller;

import com.lianzhi.wms.model.Permission;
import com.lianzhi.wms.model.Role;
import com.lianzhi.wms.repository.PermissionRepository;
import com.lianzhi.wms.repository.RoleRepository;
import jakarta.validation.Valid;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {
  private final RoleRepository repository;
  private final PermissionRepository permissionRepository;

  public RoleController(RoleRepository repository, PermissionRepository permissionRepository) {
    this.repository = repository;
    this.permissionRepository = permissionRepository;
  }

  @GetMapping
  public List<Role> list() {
    return repository.findAll();
  }

  @GetMapping("/{id}")
  public Role get(@PathVariable("id") Long id) {
    return repository.findById(id).orElseThrow();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Role create(@Valid @RequestBody Role role) {
    role.setPermissions(resolvePermissions(role.getPermissions()));
    return repository.save(role);
  }

  @PutMapping("/{id}")
  public Role update(@PathVariable("id") Long id, @Valid @RequestBody Role role) {
    role.setPermissions(resolvePermissions(role.getPermissions()));
    role.setId(id);
    return repository.save(role);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable("id") Long id) {
    repository.deleteById(id);
  }

  private Set<Permission> resolvePermissions(Set<Permission> requestedPermissions) {
    if (requestedPermissions == null || requestedPermissions.isEmpty()) {
      return new LinkedHashSet<>();
    }
    List<Long> permissionIds = requestedPermissions.stream()
        .map(Permission::getId)
        .filter(id -> id != null)
        .toList();
    if (permissionIds.size() != requestedPermissions.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "权限ID不能为空");
    }
    List<Permission> found = permissionRepository.findAllById(permissionIds);
    if (found.size() != permissionIds.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "存在无效权限ID");
    }
    return new LinkedHashSet<>(found);
  }
}
