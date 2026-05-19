package com.lianzhi.wms.controller;

import com.lianzhi.wms.model.OutboundOrder;
import com.lianzhi.wms.model.Product;
import com.lianzhi.wms.model.Warehouse;
import com.lianzhi.wms.repository.OutboundOrderRepository;
import com.lianzhi.wms.service.WarehouseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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
@RequestMapping("/warehouses")
@PreAuthorize("isAuthenticated()")
public class WarehouseController {
  private final WarehouseService service;
  private final OutboundOrderRepository outboundOrderRepository;

  public WarehouseController(WarehouseService service, OutboundOrderRepository outboundOrderRepository) {
    this.service = service;
    this.outboundOrderRepository = outboundOrderRepository;
  }

  @GetMapping
  public List<Warehouse> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  public Warehouse get(@PathVariable("id") Long id) {
    return service.get(id).orElseThrow();
  }

  @GetMapping("/{id}/outbound-products")
  public List<OutboundProductSummary> outboundProducts(@PathVariable("id") Long id) {
    service.get(id).orElseThrow();
    Map<Long, List<OutboundOrder>> grouped = outboundOrderRepository.findByWarehouseId(id).stream()
        .filter(order -> order.getProduct() != null && order.getProduct().getId() != null)
        .collect(Collectors.groupingBy(order -> order.getProduct().getId()));

    return grouped.values().stream()
        .map(orders -> {
          OutboundOrder first = orders.get(0);
          Product product = first.getProduct();
          int totalQty = orders.stream().map(OutboundOrder::getQuantity).filter(Objects::nonNull)
              .mapToInt(Integer::intValue).sum();
          return new OutboundProductSummary(
              product.getId(),
              product.getCode(),
              product.getName(),
              totalQty,
              orders.size()
          );
        })
        .sorted(Comparator.comparing(OutboundProductSummary::totalQuantity).reversed())
        .toList();
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public Warehouse create(@Valid @RequestBody WarehouseRequest request) {
    return service.create(toWarehouse(request), request.managerIds());
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public Warehouse update(@PathVariable("id") Long id, @Valid @RequestBody WarehouseRequest request) {
    return service.update(id, toWarehouse(request), request.managerIds());
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable("id") Long id) {
    service.delete(id);
  }

  public record OutboundProductSummary(
      Long productId,
      String productCode,
      String productName,
      Integer totalQuantity,
      Integer orderCount
  ) {}

  public record WarehouseRequest(
      @NotBlank(message = "仓库编号不能为空") String code,
      @NotBlank(message = "仓库名称不能为空") String name,
      @NotBlank(message = "地址不能为空") String address,
      @NotBlank(message = "仓库类型不能为空") String type,
      @NotNull(message = "启用状态不能为空") Boolean enabled,
      @NotEmpty(message = "请至少选择一位负责员工") List<Long> managerIds
  ) {}

  private Warehouse toWarehouse(WarehouseRequest request) {
    Warehouse warehouse = new Warehouse();
    warehouse.setCode(request.code());
    warehouse.setName(request.name());
    warehouse.setAddress(request.address());
    warehouse.setType(request.type());
    warehouse.setEnabled(request.enabled());
    return warehouse;
  }
}
