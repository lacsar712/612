package com.lianzhi.wms.service;

import com.lianzhi.wms.model.FlowType;
import com.lianzhi.wms.model.InboundOrder;
import com.lianzhi.wms.model.InboundStatus;
import com.lianzhi.wms.model.StockFlow;
import com.lianzhi.wms.repository.EmployeeRepository;
import com.lianzhi.wms.repository.InboundOrderRepository;
import com.lianzhi.wms.repository.ProductRepository;
import com.lianzhi.wms.repository.StockFlowRepository;
import com.lianzhi.wms.repository.SupplierRepository;
import com.lianzhi.wms.repository.WarehouseRepository;
import com.lianzhi.wms.security.CurrentUserService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InboundOrderService {
  private final InboundOrderRepository inboundOrderRepository;
  private final StockFlowRepository stockFlowRepository;
  private final WarehouseRepository warehouseRepository;
  private final SupplierRepository supplierRepository;
  private final ProductRepository productRepository;
  private final EmployeeRepository employeeRepository;
  private final CurrentUserService currentUserService;

  public InboundOrderService(InboundOrderRepository inboundOrderRepository,
                             StockFlowRepository stockFlowRepository,
                             WarehouseRepository warehouseRepository,
                             SupplierRepository supplierRepository,
                             ProductRepository productRepository,
                             EmployeeRepository employeeRepository,
                             CurrentUserService currentUserService) {
    this.inboundOrderRepository = inboundOrderRepository;
    this.stockFlowRepository = stockFlowRepository;
    this.warehouseRepository = warehouseRepository;
    this.supplierRepository = supplierRepository;
    this.productRepository = productRepository;
    this.employeeRepository = employeeRepository;
    this.currentUserService = currentUserService;
  }

  @Transactional
  public InboundOrder create(InboundOrder inboundOrder) {
    normalizeRelations(inboundOrder);
    validateCreatorAccess(inboundOrder.getCreatedBy().getId());
    if (inboundOrder.getStatus() == null) {
      inboundOrder.setStatus(InboundStatus.DRAFT);
    }
    validateEditableStatus(inboundOrder.getStatus());
    if (inboundOrder.getInboundTime() == null) {
      inboundOrder.setInboundTime(LocalDateTime.now());
    }
    InboundOrder saved = inboundOrderRepository.save(inboundOrder);
    createFlow(saved);
    return saved;
  }

  public List<InboundOrder> list() {
    return inboundOrderRepository.findAll();
  }

  public Optional<InboundOrder> get(Long id) {
    return inboundOrderRepository.findById(id);
  }

  @Transactional
  public InboundOrder update(Long id, InboundOrder inboundOrder) {
    InboundOrder existing = inboundOrderRepository.findById(id).orElseThrow();
    normalizeRelations(inboundOrder);
    validateCreatorAccess(inboundOrder.getCreatedBy().getId());
    validateUpdateAccess(existing);
    validateEditableStatus(inboundOrder.getStatus());
    inboundOrder.setId(id);
    return inboundOrderRepository.save(inboundOrder);
  }

  @Transactional
  public InboundOrder audit(Long id, InboundStatus status, String remark) {
    if (status != InboundStatus.APPROVED && status != InboundStatus.REJECTED) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "审核状态只能为 APPROVED 或 REJECTED");
    }
    return updateStatus(id, status, remark);
  }

  @Transactional
  public InboundOrder updateStatus(Long id, InboundStatus status, String remark) {
    if (status == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "状态不能为空");
    }
    InboundOrder existing = inboundOrderRepository.findById(id).orElseThrow();
    existing.setStatus(status);
    if (remark != null) {
      existing.setRemark(remark);
    }
    return inboundOrderRepository.save(existing);
  }

  public void delete(Long id) {
    InboundOrder existing = inboundOrderRepository.findById(id).orElseThrow();
    validateDeleteAccess(existing);
    inboundOrderRepository.delete(existing);
  }

  private void createFlow(InboundOrder inboundOrder) {
    StockFlow flow = new StockFlow();
    flow.setFlowNo("INF-" + UUID.randomUUID().toString().substring(0, 8));
    flow.setFlowType(FlowType.INBOUND);
    flow.setQuantity(inboundOrder.getQuantity());
    flow.setFlowTime(LocalDateTime.now());
    flow.setRefType("INBOUND");
    flow.setRefNo(inboundOrder.getOrderNo());
    flow.setWarehouse(inboundOrder.getWarehouse());
    flow.setProduct(inboundOrder.getProduct());
    flow.setOperator(inboundOrder.getCreatedBy());
    stockFlowRepository.save(flow);
  }

  private void normalizeRelations(InboundOrder inboundOrder) {
    if (inboundOrder.getWarehouse() == null || inboundOrder.getWarehouse().getId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仓库不能为空");
    }
    if (inboundOrder.getSupplier() == null || inboundOrder.getSupplier().getId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "供应商不能为空");
    }
    if (inboundOrder.getProduct() == null || inboundOrder.getProduct().getId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品不能为空");
    }
    if (inboundOrder.getCreatedBy() == null || inboundOrder.getCreatedBy().getId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "创建人不能为空");
    }
    inboundOrder.setWarehouse(warehouseRepository.findById(inboundOrder.getWarehouse().getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "仓库不存在")));
    inboundOrder.setSupplier(supplierRepository.findById(inboundOrder.getSupplier().getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "供应商不存在")));
    inboundOrder.setProduct(productRepository.findById(inboundOrder.getProduct().getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品不存在")));
    inboundOrder.setCreatedBy(employeeRepository.findById(inboundOrder.getCreatedBy().getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "创建人不存在")));
  }

  private void validateCreatorAccess(Long createdByEmployeeId) {
    if (!currentUserService.isStaff()) {
      return;
    }
    currentUserService.ensureOwnEmployee(createdByEmployeeId);
  }

  private void validateUpdateAccess(InboundOrder existing) {
    if (!currentUserService.isStaff()) {
      return;
    }
    Long currentEmployeeId = currentUserService.requireEmployeeId();
    Long creatorId = existing.getCreatedBy() == null ? null : existing.getCreatedBy().getId();
    if (!Objects.equals(currentEmployeeId, creatorId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "普通员工只能修改自己创建的入库单");
    }
    if (existing.getStatus() == InboundStatus.APPROVED || existing.getStatus() == InboundStatus.REJECTED) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "已审核入库单不能再修改");
    }
  }

  private void validateDeleteAccess(InboundOrder existing) {
    if (!currentUserService.isStaff()) {
      return;
    }
    validateUpdateAccess(existing);
  }

  private void validateEditableStatus(InboundStatus status) {
    if (!currentUserService.isStaff()) {
      return;
    }
    if (status == InboundStatus.APPROVED || status == InboundStatus.REJECTED) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "普通员工不能直接审核入库单");
    }
  }
}
