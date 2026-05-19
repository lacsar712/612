package com.lianzhi.wms.repository;

import com.lianzhi.wms.model.OutboundOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboundOrderRepository extends JpaRepository<OutboundOrder, Long> {
  List<OutboundOrder> findByProductId(Long productId);

  List<OutboundOrder> findByWarehouseId(Long warehouseId);

  boolean existsByWarehouseId(Long warehouseId);
}
