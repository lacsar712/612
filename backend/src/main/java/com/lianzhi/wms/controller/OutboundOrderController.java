package com.lianzhi.wms.controller;

import com.lianzhi.wms.model.OutboundOrder;
import com.lianzhi.wms.model.OutboundStatus;
import com.lianzhi.wms.service.OutboundOrderService;
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
@RequestMapping("/outbound-orders")
@PreAuthorize("isAuthenticated()")
public class OutboundOrderController {
  private final OutboundOrderService service;

  public OutboundOrderController(OutboundOrderService service) {
    this.service = service;
  }

  @GetMapping
  public List<OutboundOrder> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  public OutboundOrder get(@PathVariable("id") Long id) {
    return service.get(id).orElseThrow();
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
  @ResponseStatus(HttpStatus.CREATED)
  public OutboundOrder create(@Valid @RequestBody OutboundOrder outboundOrder) {
    return service.create(outboundOrder);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
  public OutboundOrder update(@PathVariable("id") Long id, @Valid @RequestBody OutboundOrder outboundOrder) {
    return service.update(id, outboundOrder);
  }

  @PutMapping("/{id}/audit")
  @PreAuthorize("hasAnyRole('ADMIN','KEEPER')")
  public OutboundOrder audit(@PathVariable("id") Long id, @Valid @RequestBody StatusRequest request) {
    return service.audit(id, request.status(), request.remark());
  }

  @PutMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('ADMIN','KEEPER')")
  public OutboundOrder updateStatus(@PathVariable("id") Long id, @Valid @RequestBody StatusRequest request) {
    return service.updateStatus(id, request.status(), request.remark());
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable("id") Long id) {
    service.delete(id);
  }

  public record StatusRequest(
      @NotNull(message = "状态不能为空") OutboundStatus status,
      String remark
  ) {}
}
