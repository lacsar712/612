package com.lianzhi.wms.service;

import com.lianzhi.wms.model.Employee;
import com.lianzhi.wms.model.Warehouse;
import com.lianzhi.wms.repository.EmployeeRepository;
import com.lianzhi.wms.repository.InboundOrderRepository;
import com.lianzhi.wms.repository.OutboundOrderRepository;
import com.lianzhi.wms.repository.StockCheckRepository;
import com.lianzhi.wms.repository.StockFlowRepository;
import com.lianzhi.wms.repository.WarehouseRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WarehouseService {
  private final WarehouseRepository warehouseRepository;
  private final EmployeeRepository employeeRepository;
  private final InboundOrderRepository inboundOrderRepository;
  private final OutboundOrderRepository outboundOrderRepository;
  private final StockCheckRepository stockCheckRepository;
  private final StockFlowRepository stockFlowRepository;

  public WarehouseService(WarehouseRepository warehouseRepository,
                          EmployeeRepository employeeRepository,
                          InboundOrderRepository inboundOrderRepository,
                          OutboundOrderRepository outboundOrderRepository,
                          StockCheckRepository stockCheckRepository,
                          StockFlowRepository stockFlowRepository) {
    this.warehouseRepository = warehouseRepository;
    this.employeeRepository = employeeRepository;
    this.inboundOrderRepository = inboundOrderRepository;
    this.outboundOrderRepository = outboundOrderRepository;
    this.stockCheckRepository = stockCheckRepository;
    this.stockFlowRepository = stockFlowRepository;
  }

  public List<Warehouse> list() {
    return warehouseRepository.findAll().stream().map(this::withManagers).toList();
  }

  public Optional<Warehouse> get(Long id) {
    return warehouseRepository.findById(id).map(this::withManagers);
  }

  @Transactional
  public Warehouse create(Warehouse request, List<Long> managerIds) {
    validateWarehouseCode(request.getCode(), null);
    Warehouse warehouse = new Warehouse();
    copyBasicFields(request, warehouse);
    if (warehouse.getEnabled() == null) {
      warehouse.setEnabled(true);
    }
    Warehouse saved = warehouseRepository.save(warehouse);
    Set<Long> resolvedManagerIds = extractManagerIds(managerIds);
    if (resolvedManagerIds.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请至少选择一位负责员工");
    }
    assignManagers(saved, resolvedManagerIds);
    return withManagers(saved);
  }

  @Transactional
  public Warehouse update(Long id, Warehouse request, List<Long> managerIds) {
    Warehouse warehouse = warehouseRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "仓库不存在"));
    validateWarehouseCode(request.getCode(), id);
    copyBasicFields(request, warehouse);
    if (warehouse.getEnabled() == null) {
      warehouse.setEnabled(true);
    }
    Warehouse saved = warehouseRepository.save(warehouse);
    Set<Long> resolvedManagerIds = extractManagerIds(managerIds);
    if (resolvedManagerIds.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请至少选择一位负责员工");
    }
    assignManagers(saved, resolvedManagerIds);
    return withManagers(saved);
  }

  @Transactional
  public void delete(Long id) {
    Warehouse warehouse = warehouseRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "仓库不存在"));
    List<Employee> currentManagers = employeeRepository.findByManagedWarehouseId(id);
    for (Employee employee : currentManagers) {
      employee.setManagedWarehouse(null);
    }
    employeeRepository.saveAll(currentManagers);
    if (inboundOrderRepository.existsByWarehouseId(id)
        || outboundOrderRepository.existsByWarehouseId(id)
        || stockCheckRepository.existsByWarehouseId(id)
        || stockFlowRepository.existsByWarehouseId(id)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "仓库存在出入库/盘点/流水记录，无法删除"
      );
    }
    warehouseRepository.delete(warehouse);
  }

  private Warehouse withManagers(Warehouse warehouse) {
    warehouse.setManagers(employeeRepository.findByManagedWarehouseId(warehouse.getId()));
    return warehouse;
  }

  private void copyBasicFields(Warehouse source, Warehouse target) {
    target.setCode(source.getCode());
    target.setName(source.getName());
    target.setAddress(source.getAddress());
    target.setType(source.getType());
    target.setEnabled(source.getEnabled());
  }

  private Set<Long> extractManagerIds(List<Long> managerIds) {
    if (managerIds == null) {
      return Set.of();
    }
    return managerIds.stream()
        .filter(id -> id != null)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private void assignManagers(Warehouse warehouse, Set<Long> managerIds) {
    List<Employee> currentManagers = employeeRepository.findByManagedWarehouseId(warehouse.getId());
    Set<Long> currentIds = currentManagers.stream().map(Employee::getId).collect(Collectors.toSet());
    List<Employee> toSave = new ArrayList<>();

    for (Employee employee : currentManagers) {
      if (!managerIds.contains(employee.getId())) {
        employee.setManagedWarehouse(null);
        toSave.add(employee);
      }
    }

    for (Long managerId : managerIds) {
      Employee employee = employeeRepository.findById(managerId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "负责人员工不存在: " + managerId));
      if (employee.getManagedWarehouse() != null
          && !warehouse.getId().equals(employee.getManagedWarehouse().getId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "员工[" + employee.getName() + "]已负责其他仓库");
      }
      if (!currentIds.contains(managerId)) {
        employee.setManagedWarehouse(warehouse);
        toSave.add(employee);
      }
    }

    if (!toSave.isEmpty()) {
      employeeRepository.saveAll(toSave);
    }
  }

  private void validateWarehouseCode(String code, Long currentId) {
    boolean exists = currentId == null
        ? warehouseRepository.existsByCode(code)
        : warehouseRepository.existsByCodeAndIdNot(code, currentId);
    if (exists) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仓库编号已存在");
    }
  }
}
