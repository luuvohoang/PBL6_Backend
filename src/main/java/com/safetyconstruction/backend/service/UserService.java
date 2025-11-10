package com.safetyconstruction.backend.service;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashSet;
import java.util.List;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.safetyconstruction.backend.dto.request.UserCreationRequest;
import com.safetyconstruction.backend.dto.request.UserUpdateRequest;
import com.safetyconstruction.backend.dto.response.UserResponse;
import com.safetyconstruction.backend.entity.User;
import com.safetyconstruction.backend.enums.Role;
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

        User user = userMapper.toUser(request);

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        HashSet<String> roles = new HashSet<>();
        roles.add(Role.USER.name());
        //                user.setRoles(roles);
        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Lấy nguyên nhân gốc rễ (Root Cause)
            Throwable rootCause = ex.getRootCause();
            String errorMessage = null;

            if (rootCause instanceof ConstraintViolationException) {
                // Case 1: Ngoại lệ gốc rễ là Hibernate ConstraintViolationException
                ConstraintViolationException cve = (ConstraintViolationException) rootCause;
                errorMessage = cve.getSQLException().getMessage();

            } else if (rootCause instanceof SQLIntegrityConstraintViolationException) {
                // Case 2: Ngoại lệ gốc rễ là Java SQLIntegrityConstraintViolationException (phổ biến trong MySQL)
                SQLIntegrityConstraintViolationException sqlex = (SQLIntegrityConstraintViolationException) rootCause;
                errorMessage = sqlex.getMessage();
            }

            if (errorMessage != null) {
                log.error("->>>>> SQL Error Message: " + errorMessage);

                // Kiểm tra Lỗi Trùng Lặp EMAIL: Dựa vào tên khóa 'user.email'
                if (errorMessage.contains("user.email")) {
                    throw new AppException(ErrorCode.INVALID_EMAIL);
                }

                // Kiểm tra Lỗi Trùng Lặp USERNAME/Trường khác: Dựa vào tên khóa tự động (hoặc tên cột)
                else if (errorMessage.contains("for key 'user")) {
                    throw new AppException(ErrorCode.USER_EXISTED);
                }

                log.error("->>>>> Không bắt được lỗi trùng lặp chi tiết");
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
            }

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
        return userMapper.toUserResponse(
                userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found")));
    }
}
