package com.lianzhi.wms.controller;

import com.lianzhi.wms.model.InboundOrder;
import com.lianzhi.wms.model.Supplier;
import com.lianzhi.wms.repository.InboundOrderRepository;
import com.lianzhi.wms.repository.SupplierRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/suppliers")
@PreAuthorize("isAuthenticated()")
public class SupplierController {
  private final SupplierRepository repository;
  private final InboundOrderRepository inboundOrderRepository;

  public SupplierController(SupplierRepository repository,
                            InboundOrderRepository inboundOrderRepository) {
    this.repository = repository;
    this.inboundOrderRepository = inboundOrderRepository;
  }

  @GetMapping
  public List<Supplier> list() {
    return repository.findAll();
  }

  @GetMapping("/{id}")
  public Supplier get(@PathVariable("id") Long id) {
    return repository.findById(id).orElseThrow();
  }

  @GetMapping("/{id}/histories")
  public List<SupplierHistoryItem> histories(@PathVariable("id") Long id) {
    if (!repository.existsById(id)) {
      throw new java.util.NoSuchElementException();
    }
    return inboundOrderRepository.findBySupplierIdOrderByInboundTimeDesc(id).stream()
        .map(this::toHistoryItem)
        .toList();
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public Supplier create(@Valid @RequestBody Supplier supplier) {
    return repository.save(supplier);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public Supplier update(@PathVariable("id") Long id, @Valid @RequestBody Supplier supplier) {
    supplier.setId(id);
    return repository.save(supplier);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable("id") Long id) {
    repository.deleteById(id);
  }

  private SupplierHistoryItem toHistoryItem(InboundOrder order) {
    return new SupplierHistoryItem(
        order.getId(),
        order.getOrderNo(),
        order.getInboundTime(),
        order.getStatus() == null ? null : order.getStatus().name(),
        order.getQuantity(),
        order.getWarehouse() == null ? null : order.getWarehouse().getId(),
        order.getWarehouse() == null ? null : order.getWarehouse().getName(),
        order.getProduct() == null ? null : order.getProduct().getId(),
        order.getProduct() == null ? null : order.getProduct().getName()
    );
  }

  public record SupplierHistoryItem(
      Long inboundOrderId,
      String orderNo,
      java.time.LocalDateTime inboundTime,
      String status,
      Integer quantity,
      Long warehouseId,
      String warehouseName,
      Long productId,
      String productName
  ) {}
}
