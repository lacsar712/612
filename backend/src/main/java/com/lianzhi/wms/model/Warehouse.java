package com.lianzhi.wms.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "warehouses")
public class Warehouse extends BaseEntity {
  @NotBlank(message = "仓库编号不能为空")
  @Column(unique = true, nullable = false)
  private String code;

  @NotBlank(message = "仓库名称不能为空")
  @Column(nullable = false)
  private String name;

  @NotBlank(message = "地址不能为空")
  private String address;

  @NotBlank(message = "仓库类型不能为空")
  private String type;

  private Boolean enabled;

  @OneToMany(mappedBy = "managedWarehouse")
  @OrderBy("id asc")
  @JsonIgnoreProperties({"department", "managedWarehouse"})
  private List<Employee> managers = new ArrayList<>();
}
