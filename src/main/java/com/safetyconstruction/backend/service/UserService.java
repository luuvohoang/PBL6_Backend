package com.safetyconstruction.backend.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.safetyconstruction.backend.dto.request.UserCreationRequest;
import com.safetyconstruction.backend.dto.request.UserUpdateRequest;
import com.safetyconstruction.backend.dto.response.UserResponse;
import com.safetyconstruction.backend.entity.Role;
import com.safetyconstruction.backend.entity.User;
import com.safetyconstruction.backend.exception.AppException;
import com.safetyconstruction.backend.exception.ErrorCode;
import com.safetyconstruction.backend.mapper.UserMapper;
import com.safetyconstruction.backend.repository.RoleRepository;
import com.safetyconstruction.backend.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    public UserResponse createUser(UserCreationRequest request) {
        log.info("Service Creating user");

        // --- BƯỚC 1: VALIDATE CHỦ ĐỘNG (Cách "Hiện tại") ---
        // (Nhanh hơn, sạch hơn, và dễ test hơn là dùng try-catch)

        if (userRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.INVALID_EMAIL);
        }
        // --- HẾT BƯỚC 1 ---

        // --- BƯỚC 2: MAP VÀ GÁN DỮ LIỆU (Logic của bạn đã đúng) ---
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Gán Role "USER" mặc định (Giả định Role là Entity)
        Role userRole = roleRepository
                .findByName("USER")
                .orElseThrow(() -> new RuntimeException("FATAL: Default USER Role not found in database."));

        // (Bỏ comment dòng user.setRoles của bạn)
        user.setRoles(new HashSet<>(Collections.singleton(userRole)));

        // --- BƯỚC 3: LƯU (VỚI TRY...CATCH TỐI GIẢN) ---
        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Lỗi này KHÔNG NÊN xảy ra (vì đã check ở Bước 1),
            // trừ khi có 2 request trùng nhau CÙNG MỘT LÚC (Race Condition)
            log.error("->>>>> Race Condition Detected or Unhandled Constraint: {}", ex.getMessage());

            // Ném ra lỗi chung chung vì đây là trường hợp hiếm
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        return userMapper.toUserResponse(user);
    }

    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByName(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)); // Dùng AppException

        // Mapper sẽ cập nhật các trường không phải mật khẩu (email, status, locale)
        userMapper.updateUser(user, request);

        // --- SỬA LỖI LOGIC ---
        // Chỉ cập nhật mật khẩu NẾU một mật khẩu mới được cung cấp
        // (Không null VÀ không rỗng)
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            log.info("Updating password for user {}", userId);
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        // Nếu không, chúng ta không làm gì cả -> giữ nguyên mật khẩu cũ trong CSDL.
        // --- HẾT SỬA ---

        // Cập nhật Roles
        var roles = roleRepository.findAllById(request.getRoles());
        user.setRoles(new HashSet<>(roles));

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getUsers() {
        log.info("Get all users");
        return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
    }

    @PostAuthorize("hasRole('ADMIN') or returnObject.name == authentication.name")
    public UserResponse getUser(String id) {
        return userMapper.toUserResponse(userRepository
                .findById(id)
                // SỬA LỖI: Ném ra AppException (404)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }
}
