package com.lianzhi.wms.controller;

import com.lianzhi.wms.model.StockFlow;
import com.lianzhi.wms.repository.StockFlowRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stock-flows")
@PreAuthorize("isAuthenticated()")
public class StockFlowController {
  private final StockFlowRepository repository;

  public StockFlowController(StockFlowRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  public List<StockFlow> list() {
    return repository.findAll();
  }

  @GetMapping("/{id}")
  public StockFlow get(@PathVariable("id") Long id) {
    return repository.findById(id).orElseThrow();
  }
}
