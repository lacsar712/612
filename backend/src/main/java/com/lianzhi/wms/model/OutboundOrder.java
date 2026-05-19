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
@Table(name = "outbound_orders")
public class OutboundOrder extends BaseEntity {
  @NotBlank(message = "出库单号不能为空")
  @Column(unique = true, nullable = false)
  private String orderNo;

  @NotNull(message = "数量不能为空")
  @Positive(message = "数量必须大于0")
  private Integer quantity;

  @NotBlank(message = "出库类型不能为空")
  private String outboundType;

  @Enumerated(EnumType.STRING)
  private OutboundStatus status;

  private LocalDateTime outboundTime;
  private String remark;

  @NotNull(message = "仓库不能为空")
  @ManyToOne
  @JoinColumn(name = "warehouse_id")
  private Warehouse warehouse;

  @NotNull(message = "商品不能为空")
  @ManyToOne
  @JoinColumn(name = "product_id")
  private Product product;

  @NotNull(message = "创建人不能为空")
  @ManyToOne
  @JoinColumn(name = "created_by")
  private Employee createdBy;
}
