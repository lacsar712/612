package com.lianzhi.wms.repository;

import com.lianzhi.wms.model.StockFlow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockFlowRepository extends JpaRepository<StockFlow, Long> {
  boolean existsByWarehouseId(Long warehouseId);
}
