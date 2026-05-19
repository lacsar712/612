package com.lianzhi.wms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "permissions")
public class Permission extends BaseEntity {
  @NotBlank(message = "权限编码不能为空")
  @Column(unique = true, nullable = false)
  private String code;

  @NotBlank(message = "权限名称不能为空")
  @Column(nullable = false)
  private String name;

  private String description;

  @JsonIgnore
  @ManyToMany(mappedBy = "permissions")
  private Set<Role> roles = new LinkedHashSet<>();
}
