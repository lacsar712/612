package com.lianzhi.wms.repository;

import com.lianzhi.wms.model.Employee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
  List<Employee> findByManagedWarehouseId(Long warehouseId);

  Optional<Employee> findByEmpNo(String empNo);
}
