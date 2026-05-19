package com.lianzhi.wms.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "employees")
public class Employee extends BaseEntity {
  @NotBlank(message = "工号不能为空")
  @Column(unique = true, nullable = false)
  private String empNo;

  @NotBlank(message = "姓名不能为空")
  @Column(nullable = false)
  private String name;

  @NotBlank(message = "性别不能为空")
  private String gender;

  private LocalDate birthDate;

  @NotBlank(message = "联系电话不能为空")
  @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
  private String phone;

  @NotBlank(message = "电子邮箱不能为空")
  @Email(message = "电子邮箱格式不正确")
  private String email;

  @NotNull(message = "角色类型不能为空")
  @Enumerated(EnumType.STRING)
  private RoleType roleType;

  @ManyToOne
  @JoinColumn(name = "department_id")
  private Department department;

  @ManyToOne
  @JoinColumn(name = "managed_warehouse_id")
  @JsonIgnoreProperties({"managers"})
  private Warehouse managedWarehouse;
}
