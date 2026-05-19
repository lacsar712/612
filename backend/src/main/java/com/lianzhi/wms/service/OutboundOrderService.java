package com.lianzhi.wms.service;

import com.lianzhi.wms.model.FlowType;
import com.lianzhi.wms.model.OutboundOrder;
import com.lianzhi.wms.model.OutboundStatus;
import com.lianzhi.wms.model.StockFlow;
import com.lianzhi.wms.repository.EmployeeRepository;
import com.lianzhi.wms.repository.OutboundOrderRepository;
import com.lianzhi.wms.repository.ProductRepository;
import com.lianzhi.wms.repository.StockFlowRepository;
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
public class OutboundOrderService {
  private final OutboundOrderRepository outboundOrderRepository;
  private final StockFlowRepository stockFlowRepository;
  private final WarehouseRepository warehouseRepository;
  private final ProductRepository productRepository;
  private final EmployeeRepository employeeRepository;
  private final CurrentUserService currentUserService;

  public OutboundOrderService(OutboundOrderRepository outboundOrderRepository,
                              StockFlowRepository stockFlowRepository,
                              WarehouseRepository warehouseRepository,
                              ProductRepository productRepository,
                              EmployeeRepository employeeRepository,
                              CurrentUserService currentUserService) {
    this.outboundOrderRepository = outboundOrderRepository;
    this.stockFlowRepository = stockFlowRepository;
    this.warehouseRepository = warehouseRepository;
    this.productRepository = productRepository;
    this.employeeRepository = employeeRepository;
    this.currentUserService = currentUserService;
  }

  @Transactional
  public OutboundOrder create(OutboundOrder outboundOrder) {
    normalizeRelations(outboundOrder);
    validateCreatorAccess(outboundOrder.getCreatedBy().getId());
    if (outboundOrder.getStatus() == null) {
      outboundOrder.setStatus(OutboundStatus.DRAFT);
    }
    validateEditableStatus(outboundOrder.getStatus());
    if (outboundOrder.getOutboundTime() == null) {
      outboundOrder.setOutboundTime(LocalDateTime.now());
    }
    OutboundOrder saved = outboundOrderRepository.save(outboundOrder);
    createFlow(saved);
    return saved;
  }

  public List<OutboundOrder> list() {
    return outboundOrderRepository.findAll();
  }

  public Optional<OutboundOrder> get(Long id) {
    return outboundOrderRepository.findById(id);
  }

  @Transactional
  public OutboundOrder update(Long id, OutboundOrder outboundOrder) {
    OutboundOrder existing = outboundOrderRepository.findById(id).orElseThrow();
    normalizeRelations(outboundOrder);
    validateCreatorAccess(outboundOrder.getCreatedBy().getId());
    validateUpdateAccess(existing);
    validateEditableStatus(outboundOrder.getStatus());
    outboundOrder.setId(id);
    return outboundOrderRepository.save(outboundOrder);
  }

  @Transactional
  public OutboundOrder audit(Long id, OutboundStatus status, String remark) {
    if (status != OutboundStatus.APPROVED && status != OutboundStatus.REJECTED) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "审核状态只能为 APPROVED 或 REJECTED");
    }
    return updateStatus(id, status, remark);
  }

  @Transactional
  public OutboundOrder updateStatus(Long id, OutboundStatus status, String remark) {
    if (status == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "状态不能为空");
    }
    OutboundOrder existing = outboundOrderRepository.findById(id).orElseThrow();
    existing.setStatus(status);
    if (remark != null) {
      existing.setRemark(remark);
    }
    return outboundOrderRepository.save(existing);
  }

  public void delete(Long id) {
    OutboundOrder existing = outboundOrderRepository.findById(id).orElseThrow();
    validateDeleteAccess(existing);
    outboundOrderRepository.delete(existing);
  }

  private void createFlow(OutboundOrder outboundOrder) {
    StockFlow flow = new StockFlow();
    flow.setFlowNo("OUT-" + UUID.randomUUID().toString().substring(0, 8));
    flow.setFlowType(FlowType.OUTBOUND);
    flow.setQuantity(outboundOrder.getQuantity());
    flow.setFlowTime(LocalDateTime.now());
    flow.setRefType("OUTBOUND");
    flow.setRefNo(outboundOrder.getOrderNo());
    flow.setWarehouse(outboundOrder.getWarehouse());
    flow.setProduct(outboundOrder.getProduct());
    flow.setOperator(outboundOrder.getCreatedBy());
    stockFlowRepository.save(flow);
  }

  private void normalizeRelations(OutboundOrder outboundOrder) {
    if (outboundOrder.getWarehouse() == null || outboundOrder.getWarehouse().getId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仓库不能为空");
    }
    if (outboundOrder.getProduct() == null || outboundOrder.getProduct().getId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品不能为空");
    }
    if (outboundOrder.getCreatedBy() == null || outboundOrder.getCreatedBy().getId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "创建人不能为空");
    }
    outboundOrder.setWarehouse(warehouseRepository.findById(outboundOrder.getWarehouse().getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "仓库不存在")));
    outboundOrder.setProduct(productRepository.findById(outboundOrder.getProduct().getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品不存在")));
    outboundOrder.setCreatedBy(employeeRepository.findById(outboundOrder.getCreatedBy().getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "创建人不存在")));
  }

  private void validateCreatorAccess(Long createdByEmployeeId) {
    if (!currentUserService.isStaff()) {
      return;
    }
    currentUserService.ensureOwnEmployee(createdByEmployeeId);
  }

  private void validateUpdateAccess(OutboundOrder existing) {
    if (!currentUserService.isStaff()) {
      return;
    }
    Long currentEmployeeId = currentUserService.requireEmployeeId();
    Long creatorId = existing.getCreatedBy() == null ? null : existing.getCreatedBy().getId();
    if (!Objects.equals(currentEmployeeId, creatorId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "普通员工只能修改自己创建的出库单");
    }
    if (existing.getStatus() == OutboundStatus.APPROVED || existing.getStatus() == OutboundStatus.REJECTED) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "已审核出库单不能再修改");
    }
  }

  private void validateDeleteAccess(OutboundOrder existing) {
    if (!currentUserService.isStaff()) {
      return;
    }
    validateUpdateAccess(existing);
  }

  private void validateEditableStatus(OutboundStatus status) {
    if (!currentUserService.isStaff()) {
      return;
    }
    if (status == OutboundStatus.APPROVED || status == OutboundStatus.REJECTED) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "普通员工不能直接审核出库单");
    }
  }
}
