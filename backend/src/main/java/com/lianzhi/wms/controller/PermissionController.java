package com.lianzhi.wms.controller;

import com.lianzhi.wms.model.Permission;
import com.lianzhi.wms.repository.PermissionRepository;
import jakarta.validation.Valid;
import java.util.List;
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

@RestController
@RequestMapping("/permissions")
@PreAuthorize("hasRole('ADMIN')")
public class PermissionController {
  private final PermissionRepository repository;

  public PermissionController(PermissionRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  public List<Permission> list() {
    return repository.findAll();
  }

  @GetMapping("/{id}")
  public Permission get(@PathVariable("id") Long id) {
    return repository.findById(id).orElseThrow();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Permission create(@Valid @RequestBody Permission permission) {
    return repository.save(permission);
  }

  @PutMapping("/{id}")
  public Permission update(@PathVariable("id") Long id, @Valid @RequestBody Permission permission) {
    permission.setId(id);
    return repository.save(permission);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable("id") Long id) {
    repository.deleteById(id);
  }
}
