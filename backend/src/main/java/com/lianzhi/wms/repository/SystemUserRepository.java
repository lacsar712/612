package com.lianzhi.wms.repository;

import com.lianzhi.wms.model.SystemUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemUserRepository extends JpaRepository<SystemUser, Long> {
  Optional<SystemUser> findByUsername(String username);

  boolean existsByUsername(String username);

  Optional<SystemUser> findByEmployeeId(Long employeeId);
}
