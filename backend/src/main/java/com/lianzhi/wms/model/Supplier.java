package com.lianzhi.wms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "suppliers")
public class Supplier extends BaseEntity {
  @NotBlank(message = "供应商编号不能为空")
  @Column(unique = true, nullable = false)
  private String code;

  @NotBlank(message = "供应商名称不能为空")
  @Column(nullable = false)
  private String name;

  @NotBlank(message = "联系人不能为空")
  private String contactName;

  @NotBlank(message = "联系电话不能为空")
  @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
  private String phone;

  @NotBlank(message = "电子邮箱不能为空")
  @Email(message = "电子邮箱格式不正确")
  private String email;

  @NotBlank(message = "供货类型不能为空")
  private String supplyType;

  @NotBlank(message = "合作类型不能为空")
  private String cooperationType;

  @NotBlank(message = "合作状态不能为空")
  private String status;
}
