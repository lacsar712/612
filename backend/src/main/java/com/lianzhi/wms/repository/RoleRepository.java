package com.lianzhi.wms.repository;

import com.lianzhi.wms.model.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByCode(String code);
}
