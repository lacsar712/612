package com.lianzhi.wms.config;

import com.lianzhi.wms.model.Department;
import com.lianzhi.wms.model.Employee;
import com.lianzhi.wms.model.Role;
import com.lianzhi.wms.model.RoleType;
import com.lianzhi.wms.model.SystemUser;
import com.lianzhi.wms.repository.DepartmentRepository;
import com.lianzhi.wms.repository.EmployeeRepository;
import com.lianzhi.wms.repository.RoleRepository;
import com.lianzhi.wms.repository.SystemUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
  private final RoleRepository roleRepository;
  private final DepartmentRepository departmentRepository;
  private final EmployeeRepository employeeRepository;
  private final SystemUserRepository systemUserRepository;
  private final PasswordEncoder passwordEncoder;

  public DataInitializer(RoleRepository roleRepository,
                         DepartmentRepository departmentRepository,
                         EmployeeRepository employeeRepository,
                         SystemUserRepository systemUserRepository,
                         PasswordEncoder passwordEncoder) {
    this.roleRepository = roleRepository;
    this.departmentRepository = departmentRepository;
    this.employeeRepository = employeeRepository;
    this.systemUserRepository = systemUserRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(String... args) {
    Role adminRole = ensureRole("ADMIN", "系统管理员", "系统最高权限");
    Role keeperRole = ensureRole("KEEPER", "仓库保管员", "负责审核入库单、出库单并维护状态");
    Role staffRole = ensureRole("STAFF", "业务员工", "负责提交业务单据并查询本人信息");

    Department headquarters = ensureDepartment("HQ", "总部");
    Department warehouseDepartment = ensureDepartment("WH", "仓储部");
    Department businessDepartment = ensureDepartment("BD", "业务部");

    Employee adminEmployee = ensureEmployee(
        "E-0001",
        "系统管理员",
        "男",
        "13800000000",
        "admin@lianzhi.local",
        RoleType.ADMIN,
        headquarters
    );
    Employee keeperEmployee = ensureEmployee(
        "E-0002",
        "仓库保管员",
        "女",
        "13800000001",
        "keeper@lianzhi.local",
        RoleType.KEEPER,
        warehouseDepartment
    );
    Employee staffEmployee = ensureEmployee(
        "E-0003",
        "业务员工",
        "男",
        "13800000002",
        "staff@lianzhi.local",
        RoleType.STAFF,
        businessDepartment
    );

    ensureUser("admin", "123456", adminRole, adminEmployee);
    ensureUser("keeper", "123456", keeperRole, keeperEmployee);
    ensureUser("staff", "123456", staffRole, staffEmployee);
  }

  private Role ensureRole(String code, String name, String description) {
    return roleRepository.findByCode(code).map(role -> {
      boolean changed = false;
      if (!name.equals(role.getName())) {
        role.setName(name);
        changed = true;
      }
      if (!description.equals(role.getDescription())) {
        role.setDescription(description);
        changed = true;
      }
      return changed ? roleRepository.save(role) : role;
    }).orElseGet(() -> {
      Role role = new Role();
      role.setCode(code);
      role.setName(name);
      role.setDescription(description);
      return roleRepository.save(role);
    });
  }

  private Department ensureDepartment(String code, String name) {
    return departmentRepository.findByCode(code).map(department -> {
      if (!name.equals(department.getName())) {
        department.setName(name);
        return departmentRepository.save(department);
      }
      return department;
    }).orElseGet(() -> {
      Department department = new Department();
      department.setCode(code);
      department.setName(name);
      return departmentRepository.save(department);
    });
  }

  private Employee ensureEmployee(String empNo,
                                  String name,
                                  String gender,
                                  String phone,
                                  String email,
                                  RoleType roleType,
                                  Department department) {
    return employeeRepository.findByEmpNo(empNo).map(employee -> {
      boolean changed = false;
      if (!name.equals(employee.getName())) {
        employee.setName(name);
        changed = true;
      }
      if (!gender.equals(employee.getGender())) {
        employee.setGender(gender);
        changed = true;
      }
      if (!phone.equals(employee.getPhone())) {
        employee.setPhone(phone);
        changed = true;
      }
      if (!email.equals(employee.getEmail())) {
        employee.setEmail(email);
        changed = true;
      }
      if (employee.getRoleType() != roleType) {
        employee.setRoleType(roleType);
        changed = true;
      }
      if (employee.getDepartment() == null || !department.getId().equals(employee.getDepartment().getId())) {
        employee.setDepartment(department);
        changed = true;
      }
      return changed ? employeeRepository.save(employee) : employee;
    }).orElseGet(() -> {
      Employee employee = new Employee();
      employee.setEmpNo(empNo);
      employee.setName(name);
      employee.setGender(gender);
      employee.setPhone(phone);
      employee.setEmail(email);
      employee.setRoleType(roleType);
      employee.setDepartment(department);
      return employeeRepository.save(employee);
    });
  }

  private void ensureUser(String username, String rawPassword, Role role, Employee employee) {
    SystemUser user = systemUserRepository.findByUsername(username)
        .orElseGet(() -> {
          SystemUser systemUser = new SystemUser();
          systemUser.setUsername(username);
          return systemUser;
        });

    boolean changed = false;
    if (user.getPasswordHash() == null || !user.getPasswordHash().startsWith("$2")) {
      user.setPasswordHash(passwordEncoder.encode(rawPassword));
      changed = true;
    }
    if (!Boolean.TRUE.equals(user.getEnabled())) {
      user.setEnabled(true);
      changed = true;
    }
    if (user.getRole() == null || !role.getId().equals(user.getRole().getId())) {
      user.setRole(role);
      changed = true;
    }
    if (user.getEmployee() == null || !employee.getId().equals(user.getEmployee().getId())) {
      user.setEmployee(employee);
      changed = true;
    }
    if (user.getId() == null || changed) {
      systemUserRepository.save(user);
    }
  }
}
