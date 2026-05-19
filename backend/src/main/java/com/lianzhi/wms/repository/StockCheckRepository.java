package com.lianzhi.wms.repository;

import com.lianzhi.wms.model.StockCheck;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockCheckRepository extends JpaRepository<StockCheck, Long> {
  boolean existsByWarehouseId(Long warehouseId);
}
