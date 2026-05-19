package com.lianzhi.wms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "departments")
public class Department extends BaseEntity {
  @NotBlank(message = "部门编号不能为空")
  @Column(unique = true, nullable = false)
  private String code;

  @NotBlank(message = "部门名称不能为空")
  @Column(nullable = false)
  private String name;

  @ManyToOne
  @JoinColumn(name = "parent_id")
  private Department parent;
}
