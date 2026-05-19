package com.lianzhi.wms.repository;

import com.lianzhi.wms.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {}
