package com.lianzhi.wms.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "system_users")
public class SystemUser extends BaseEntity {
  @NotBlank(message = "用户名不能为空")
  @Column(unique = true, nullable = false)
  private String username;

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String passwordHash;

  @NotNull(message = "启用状态不能为空")
  private Boolean enabled;

  @ManyToOne
  @JoinColumn(name = "role_id")
  private Role role;

  @OneToOne
  @JoinColumn(name = "employee_id")
  private Employee employee;
}
