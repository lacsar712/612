package com.lianzhi.wms.controller;

import com.lianzhi.wms.model.Employee;
import com.lianzhi.wms.model.Product;
import com.lianzhi.wms.model.StockCheck;
import com.lianzhi.wms.model.Warehouse;
import com.lianzhi.wms.repository.EmployeeRepository;
import com.lianzhi.wms.repository.ProductRepository;
import com.lianzhi.wms.repository.StockCheckRepository;
import com.lianzhi.wms.repository.WarehouseRepository;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/stock-checks")
@PreAuthorize("isAuthenticated()")
public class StockCheckController {
  private final StockCheckRepository repository;
  private final WarehouseRepository warehouseRepository;
  private final ProductRepository productRepository;
  private final EmployeeRepository employeeRepository;

  public StockCheckController(StockCheckRepository repository,
                              WarehouseRepository warehouseRepository,
                              ProductRepository productRepository,
                              EmployeeRepository employeeRepository) {
    this.repository = repository;
    this.warehouseRepository = warehouseRepository;
    this.productRepository = productRepository;
    this.employeeRepository = employeeRepository;
  }

  @GetMapping
  public List<StockCheck> list() {
    return repository.findAll();
  }

  @GetMapping("/{id}")
  public StockCheck get(@PathVariable("id") Long id) {
    return repository.findById(id).orElseThrow();
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','KEEPER')")
  @ResponseStatus(HttpStatus.CREATED)
  public StockCheck create(@Valid @RequestBody StockCheck stockCheck) {
    normalizeRelations(stockCheck);
    return repository.save(stockCheck);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','KEEPER')")
  public StockCheck update(@PathVariable("id") Long id, @Valid @RequestBody StockCheck stockCheck) {
    normalizeRelations(stockCheck);
    stockCheck.setId(id);
    return repository.save(stockCheck);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','KEEPER')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable("id") Long id) {
    repository.deleteById(id);
  }

  private void normalizeRelations(StockCheck stockCheck) {
    if (stockCheck.getWarehouse() == null || stockCheck.getWarehouse().getId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仓库不能为空");
    }
    if (stockCheck.getProduct() == null || stockCheck.getProduct().getId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品不能为空");
    }
    if (stockCheck.getCreatedBy() == null || stockCheck.getCreatedBy().getId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "盘点人不能为空");
    }

    Warehouse warehouse = warehouseRepository.findById(stockCheck.getWarehouse().getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "仓库不存在"));
    Product product = productRepository.findById(stockCheck.getProduct().getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品不存在"));
    Employee employee = employeeRepository.findById(stockCheck.getCreatedBy().getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "盘点人不存在"));

    stockCheck.setWarehouse(warehouse);
    stockCheck.setProduct(product);
    stockCheck.setCreatedBy(employee);
  }
}
