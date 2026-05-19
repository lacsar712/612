package com.lianzhi.wms.controller;

import com.lianzhi.wms.model.OutboundOrder;
import com.lianzhi.wms.model.Product;
import com.lianzhi.wms.model.Warehouse;
import com.lianzhi.wms.repository.OutboundOrderRepository;
import com.lianzhi.wms.repository.ProductRepository;
import jakarta.validation.Valid;
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
@RequestMapping("/products")
@PreAuthorize("isAuthenticated()")
public class ProductController {
  private final ProductRepository repository;
  private final OutboundOrderRepository outboundOrderRepository;

  public ProductController(ProductRepository repository, OutboundOrderRepository outboundOrderRepository) {
    this.repository = repository;
    this.outboundOrderRepository = outboundOrderRepository;
  }

  @GetMapping
  public List<Product> list() {
    return repository.findAll();
  }

  @GetMapping("/{id}")
  public Product get(@PathVariable("id") Long id) {
    return repository.findById(id).orElseThrow();
  }

  @GetMapping("/{id}/outbound-warehouses")
  public List<OutboundWarehouseSummary> outboundWarehouses(@PathVariable("id") Long id) {
    repository.findById(id).orElseThrow();
    Map<Long, List<OutboundOrder>> grouped = outboundOrderRepository.findByProductId(id).stream()
        .filter(order -> order.getWarehouse() != null && order.getWarehouse().getId() != null)
        .collect(Collectors.groupingBy(order -> order.getWarehouse().getId()));

    return grouped.values().stream()
        .map(orders -> {
          OutboundOrder first = orders.get(0);
          Warehouse warehouse = first.getWarehouse();
          int totalQty = orders.stream().map(OutboundOrder::getQuantity).filter(Objects::nonNull)
              .mapToInt(Integer::intValue).sum();
          return new OutboundWarehouseSummary(
              warehouse.getId(),
              warehouse.getCode(),
              warehouse.getName(),
              totalQty,
              orders.size()
          );
        })
        .sorted(Comparator.comparing(OutboundWarehouseSummary::totalQuantity).reversed())
        .toList();
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public Product create(@Valid @RequestBody Product product) {
    return repository.save(product);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public Product update(@PathVariable("id") Long id, @Valid @RequestBody Product product) {
    product.setId(id);
    return repository.save(product);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable("id") Long id) {
    repository.deleteById(id);
  }

  public record OutboundWarehouseSummary(
      Long warehouseId,
      String warehouseCode,
      String warehouseName,
      Integer totalQuantity,
      Integer orderCount
  ) {}
}
