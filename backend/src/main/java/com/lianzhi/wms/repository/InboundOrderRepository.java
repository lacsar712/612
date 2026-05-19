package com.lianzhi.wms.repository;

import com.lianzhi.wms.model.InboundOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboundOrderRepository extends JpaRepository<InboundOrder, Long> {
  List<InboundOrder> findBySupplierIdOrderByInboundTimeDesc(Long supplierId);

  boolean existsByWarehouseId(Long warehouseId);
}
