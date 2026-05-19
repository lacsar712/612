package com.lianzhi.wms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product extends BaseEntity {
  @NotBlank(message = "商品编号不能为空")
  @Column(unique = true, nullable = false)
  private String code;

  @NotBlank(message = "商品名称不能为空")
  @Column(nullable = false)
  private String name;

  @NotBlank(message = "SKU条码不能为空")
  @Column(unique = true, nullable = false)
  private String sku;

  @NotBlank(message = "规格型号不能为空")
  private String spec;

  @NotBlank(message = "单位不能为空")
  private String unit;

  private String description;
}
