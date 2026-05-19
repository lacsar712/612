package com.lianzhi.wms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stock_checks")
public class StockCheck extends BaseEntity {
  @NotBlank(message = "盘点编号不能为空")
  @Column(unique = true, nullable = false)
  private String checkNo;

  @NotNull(message = "盘点日期不能为空")
  private LocalDate checkDate;

  @NotNull(message = "账面数量不能为空")
  @PositiveOrZero(message = "账面数量不能小于0")
  private Integer bookQty;

  @NotNull(message = "实际数量不能为空")
  @PositiveOrZero(message = "实际数量不能小于0")
  private Integer actualQty;

  @NotNull(message = "仓库不能为空")
  @ManyToOne
  @JoinColumn(name = "warehouse_id")
  private Warehouse warehouse;

  @NotNull(message = "商品不能为空")
  @ManyToOne
  @JoinColumn(name = "product_id")
  private Product product;

  @NotNull(message = "盘点人不能为空")
  @ManyToOne
  @JoinColumn(name = "created_by")
  private Employee createdBy;
}
