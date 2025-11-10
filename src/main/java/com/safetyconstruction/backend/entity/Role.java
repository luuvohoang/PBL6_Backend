package com.safetyconstruction.backend.entity;

import java.util.Set;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Role {
    @Id
    String name;

    String description;

    @ManyToMany(fetch = FetchType.EAGER) // EAGER: Luôn tải Permissions khi tải Role
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_name"), // Khóa ngoại bảng này
            inverseJoinColumns = @JoinColumn(name = "permission_name") // Khóa ngoại bảng kia
            )
    Set<Permission> permissions;
}
