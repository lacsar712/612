package com.lianzhi.wms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inbound_orders")
public class InboundOrder extends BaseEntity {
  @NotBlank(message = "入库单号不能为空")
  @Column(unique = true, nullable = false)
  private String orderNo;

  @NotNull(message = "数量不能为空")
  @Positive(message = "数量必须大于0")
  private Integer quantity;

  @NotBlank(message = "入库类型不能为空")
  private String inboundType;

  @Enumerated(EnumType.STRING)
  private InboundStatus status;

  private LocalDateTime inboundTime;
  private String remark;

  @NotNull(message = "仓库不能为空")
  @ManyToOne
  @JoinColumn(name = "warehouse_id")
  private Warehouse warehouse;

  @NotNull(message = "供应商不能为空")
  @ManyToOne
  @JoinColumn(name = "supplier_id")
  private Supplier supplier;

  @NotNull(message = "商品不能为空")
  @ManyToOne
  @JoinColumn(name = "product_id")
  private Product product;

  @NotNull(message = "创建人不能为空")
  @ManyToOne
  @JoinColumn(name = "created_by")
  private Employee createdBy;
}
