package com.safetyconstruction.backend.dto.request.role;

import lombok.*;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleUpdateRequest {
    // (Không có 'name' - không cho sửa khóa chính)
    String description;
    Set<String> permissions; // Danh sách tên các quyền mới
}
