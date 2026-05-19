package com.lianzhi.wms.controller;

import com.lianzhi.wms.model.Department;
import com.lianzhi.wms.model.Employee;
import com.lianzhi.wms.model.Warehouse;
import com.lianzhi.wms.repository.DepartmentRepository;
import com.lianzhi.wms.repository.EmployeeRepository;
import com.lianzhi.wms.repository.WarehouseRepository;
import com.lianzhi.wms.security.CurrentUserService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/employees")
@PreAuthorize("isAuthenticated()")
public class EmployeeController {
  private final EmployeeRepository repository;
  private final DepartmentRepository departmentRepository;
  private final WarehouseRepository warehouseRepository;
  private final CurrentUserService currentUserService;

  public EmployeeController(EmployeeRepository repository,
                            DepartmentRepository departmentRepository,
                            WarehouseRepository warehouseRepository,
                            CurrentUserService currentUserService) {
    this.repository = repository;
    this.departmentRepository = departmentRepository;
    this.warehouseRepository = warehouseRepository;
    this.currentUserService = currentUserService;
  }

  @GetMapping
  public List<Employee> list() {
    if (currentUserService.canViewAllEmployees()) {
      return repository.findAll();
    }
    return repository.findById(currentUserService.requireEmployeeId())
        .map(List::of)
        .orElse(List.of());
  }

  @GetMapping("/{id}")
  @PreAuthorize("@currentUser.canViewEmployee(#id)")
  public Employee get(@PathVariable("id") Long id) {
    return repository.findById(id).orElseThrow();
  }

  @GetMapping("/export")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<byte[]> export() {
    List<Employee> employees = repository.findAll();
    StringBuilder csv = new StringBuilder();
    csv.append("ID,工号,姓名,性别,出生日期,联系电话,电子邮箱,角色类型,部门,负责仓库\n");
    for (Employee employee : employees) {
      csv.append(employee.getId()).append(',')
          .append(csvValue(employee.getEmpNo())).append(',')
          .append(csvValue(employee.getName())).append(',')
          .append(csvValue(employee.getGender())).append(',')
          .append(csvValue(employee.getBirthDate() == null ? "" : employee.getBirthDate().toString())).append(',')
          .append(csvValue(employee.getPhone())).append(',')
          .append(csvValue(employee.getEmail())).append(',')
          .append(csvValue(employee.getRoleType() == null ? "" : employee.getRoleType().name())).append(',')
          .append(csvValue(employee.getDepartment() == null ? "" : employee.getDepartment().getName())).append(',')
          .append(csvValue(employee.getManagedWarehouse() == null ? "" : employee.getManagedWarehouse().getName()))
          .append('\n');
    }
    byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employees.csv\"")
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .body(body);
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public Employee create(@Valid @RequestBody Employee employee) {
    normalizeRelations(employee);
    return repository.save(employee);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public Employee update(@PathVariable("id") Long id, @Valid @RequestBody Employee employee) {
    normalizeRelations(employee);
    employee.setId(id);
    return repository.save(employee);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable("id") Long id) {
    repository.deleteById(id);
  }

  private void normalizeRelations(Employee employee) {
    if (employee.getDepartment() != null && employee.getDepartment().getId() != null) {
      Department department = departmentRepository.findById(employee.getDepartment().getId())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "部门不存在"));
      employee.setDepartment(department);
    } else {
      employee.setDepartment(null);
    }

    if (employee.getManagedWarehouse() != null && employee.getManagedWarehouse().getId() != null) {
      Warehouse warehouse = warehouseRepository.findById(employee.getManagedWarehouse().getId())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "仓库不存在"));
      employee.setManagedWarehouse(warehouse);
    } else {
      employee.setManagedWarehouse(null);
    }
  }

  private String csvValue(String value) {
    if (value == null) {
      return "";
    }
    String escaped = value.replace("\"", "\"\"");
    if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
      return "\"" + escaped + "\"";
    }
    return escaped;
  }
}
