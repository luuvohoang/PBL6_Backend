package com.safetyconstruction.backend.dto.request.role;

import java.util.Set;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleUpdateRequest {
    // (Không có 'name' - không cho sửa khóa chính)
    String description;
    Set<String> permissions; // Danh sách tên các quyền mới
}
