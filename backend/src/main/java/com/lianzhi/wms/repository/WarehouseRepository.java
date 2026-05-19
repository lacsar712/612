package com.lianzhi.wms.repository;

import com.lianzhi.wms.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
  boolean existsByCode(String code);

  boolean existsByCodeAndIdNot(String code, Long id);
}
