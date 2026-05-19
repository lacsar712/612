package com.lianzhi.wms.controller;

import com.lianzhi.wms.model.Department;
import com.lianzhi.wms.repository.DepartmentRepository;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/departments")
@PreAuthorize("hasRole('ADMIN')")
public class DepartmentController {
  private final DepartmentRepository repository;

  public DepartmentController(DepartmentRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  public List<Department> list() {
    return repository.findAll();
  }

  @GetMapping("/{id}")
  public Department get(@PathVariable("id") Long id) {
    return repository.findById(id).orElseThrow();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Department create(@Valid @RequestBody Department department) {
    normalizeParent(department, null);
    return repository.save(department);
  }

  @PutMapping("/{id}")
  public Department update(@PathVariable("id") Long id, @Valid @RequestBody Department department) {
    normalizeParent(department, id);
    department.setId(id);
    return repository.save(department);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable("id") Long id) {
    repository.deleteById(id);
  }

  private void normalizeParent(Department department, Long currentId) {
    if (department.getParent() == null) {
      return;
    }
    Long parentId = department.getParent().getId();
    if (parentId == null) {
      department.setParent(null);
      return;
    }
    if (currentId != null && currentId.equals(parentId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "上级部门不能是当前部门");
    }
    Department parent = repository.findById(parentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "上级部门不存在"));
    department.setParent(parent);
  }
}
