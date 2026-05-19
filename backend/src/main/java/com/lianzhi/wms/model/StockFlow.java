package com.lianzhi.wms.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stock_flows")
public class StockFlow extends BaseEntity {
  private String flowNo;

  @Enumerated(EnumType.STRING)
  private FlowType flowType;

  private Integer quantity;
  private LocalDateTime flowTime;
  private String refType;
  private String refNo;

  @ManyToOne
  @JoinColumn(name = "warehouse_id")
  private Warehouse warehouse;

  @ManyToOne
  @JoinColumn(name = "product_id")
  private Product product;

  @ManyToOne
  @JoinColumn(name = "operator_id")
  private Employee operator;
}
