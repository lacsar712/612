package com.lianzhi.wms.controller;

import com.lianzhi.wms.model.InboundOrder;
import com.lianzhi.wms.model.InboundStatus;
import com.lianzhi.wms.service.InboundOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/inbound-orders")
@PreAuthorize("isAuthenticated()")
public class InboundOrderController {
  private final InboundOrderService service;

  public InboundOrderController(InboundOrderService service) {
    this.service = service;
  }

  @GetMapping
  public List<InboundOrder> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  public InboundOrder get(@PathVariable("id") Long id) {
    return service.get(id).orElseThrow();
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
  @ResponseStatus(HttpStatus.CREATED)
  public InboundOrder create(@Valid @RequestBody InboundOrder inboundOrder) {
    return service.create(inboundOrder);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
  public InboundOrder update(@PathVariable("id") Long id, @Valid @RequestBody InboundOrder inboundOrder) {
    return service.update(id, inboundOrder);
  }

  @PutMapping("/{id}/audit")
  @PreAuthorize("hasAnyRole('ADMIN','KEEPER')")
  public InboundOrder audit(@PathVariable("id") Long id, @Valid @RequestBody StatusRequest request) {
    return service.audit(id, request.status(), request.remark());
  }

  @PutMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('ADMIN','KEEPER')")
  public InboundOrder updateStatus(@PathVariable("id") Long id, @Valid @RequestBody StatusRequest request) {
    return service.updateStatus(id, request.status(), request.remark());
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable("id") Long id) {
    service.delete(id);
  }

  public record StatusRequest(
      @NotNull(message = "状态不能为空") InboundStatus status,
      String remark
  ) {}
}
