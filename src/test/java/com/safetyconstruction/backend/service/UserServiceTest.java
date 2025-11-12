// File: src/test/java/com/safetyconstruction/backend/service/UserServiceTest.java
package com.safetyconstruction.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.safetyconstruction.backend.dto.request.UserCreationRequest;
import com.safetyconstruction.backend.dto.request.UserUpdateRequest;
import com.safetyconstruction.backend.dto.response.UserResponse;
import com.safetyconstruction.backend.entity.Role;
import com.safetyconstruction.backend.entity.User;
import com.safetyconstruction.backend.exception.AppException;
import com.safetyconstruction.backend.mapper.UserMapper;
import com.safetyconstruction.backend.repository.RoleRepository;
import com.safetyconstruction.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityContext securityContext; // Mock Security

    @Mock
    private Authentication authentication; // Mock Authentication

    @InjectMocks
    private UserService userService;

    // Biến mẫu (sẽ được dùng bởi các mock)
    private User sampleUser;
    private UserResponse sampleUserResponse;
    private Role sampleRole;

    // Danh sách tĩnh (static) để lưu KẾT QUẢ của TẤT CẢ các test
    private static List<Object[]> testResults = new ArrayList<>();

    @BeforeAll
    static void clearTestResults() {
        // Xóa sạch kết quả của các lần chạy TRƯỚC ĐÓ
        testResults.clear();
    }

    @BeforeEach
    void setUp() {
        // Cài đặt các đối tượng mẫu
        sampleUser = User.builder()
                .id("uuid-123")
                .name("testuser")
                .email("test@email.com")
                .roles(new HashSet<>())
                .build();

        sampleUserResponse = UserResponse.builder()
                .id("uuid-123")
                .name("testuser")
                .email("test@email.com")
                .build();

        sampleRole = Role.builder().name("USER").build();

        // --- MOCK CHUNG (COMMON MOCKS) ---
        // Giả lập (mock) các hàm dùng chung cho tất cả các test
        lenient().when(userMapper.toUser(any(UserCreationRequest.class))).thenReturn(sampleUser);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        lenient().when(userMapper.toUserResponse(any(User.class))).thenReturn(sampleUserResponse);
        lenient().when(roleRepository.findByName("USER")).thenReturn(Optional.of(sampleRole));
        lenient().when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        // Giả lập (mock) SecurityContextHolder (dùng cho 'getMyInfo')
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // --- Test 1: Đọc từ Sheet "CreateUser" ---
    @DisplayName("Test Create User (Excel)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.UserServiceExcelProvider#createUserProvider")
    void testCreateUser_FromExcel(String testName, UserCreationRequest request, String expectedError) {
        String actualResult = "PASSED";
        String actualMessage = "";

        // --- ARRANGE (Thiết lập) ---
        // (Mock logic 'existsBy...' của code thật)
        lenient()
                .when(userRepository.existsByName(request.getName()))
                .thenReturn(expectedError != null && expectedError.equals("USER_EXISTED"));
        lenient()
                .when(userRepository.existsByEmail(request.getEmail()))
                .thenReturn(expectedError != null && expectedError.equals("INVALID_EMAIL"));

        // --- ACT & ASSERT ---
        try {
            if (expectedError == null) {
                UserResponse response = userService.createUser(request);
                assertNotNull(response);
                actualMessage = "Tạo thành công (ID: " + response.getId() + ")";
            } else {
                AppException exception = assertThrows(AppException.class, () -> userService.createUser(request));
                assertEquals(expectedError, exception.getErrorCode().name());
                actualMessage = "Bắt được lỗi " + expectedError + " (ĐÚNG)";
            }
        } catch (Throwable e) {
            actualResult = "FAILED";
            actualMessage = e.getMessage().replaceAll("\n", " ");
        }
        testResults.add(new Object[] {
            testName, request.getName(), request.getEmail(), expectedError, actualResult, actualMessage
        });
    }

    // --- Test 2: Đọc từ Sheet "UpdateUser" ---
    @DisplayName("Test Update User (Excel)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.UserServiceExcelProvider#updateUserProvider")
    void testUpdateUser_FromExcel(
            String testName, String userIdToUpdate, UserUpdateRequest request, String expectedError) {
        String actualResult = "PASSED";
        String actualMessage = "";

        // --- ARRANGE ---
        // (Mock 'findById' của code thật)
        if (expectedError != null && expectedError.equals("USER_NOT_EXISTED")) {
            lenient().when(userRepository.findById(userIdToUpdate)).thenReturn(Optional.empty());
        } else {
            lenient().when(userRepository.findById(userIdToUpdate)).thenReturn(Optional.of(sampleUser));
            lenient().when(roleRepository.findAllById(any())).thenReturn(Collections.emptyList());
        }

        // --- ACT & ASSERT ---
        try {
            if (expectedError == null) {
                UserResponse response = userService.updateUser(userIdToUpdate, request);
                assertNotNull(response);
                actualMessage = "Cập nhật thành công";
            } else {
                AppException exception =
                        assertThrows(AppException.class, () -> userService.updateUser(userIdToUpdate, request));
                assertEquals(expectedError, exception.getErrorCode().name());
                actualMessage = "Bắt được lỗi " + expectedError + " (ĐÚNG)";
            }
        } catch (Throwable e) {
            actualResult = "FAILED";
            actualMessage = e.getMessage().replaceAll("\n", " ");
        }
        testResults.add(new Object[] {
            testName, "UserID: " + userIdToUpdate, request.getEmail(), expectedError, actualResult, actualMessage
        });
    }

    // --- Test 3: Đọc từ Sheet "DeleteUser" ---
    @DisplayName("Test Delete User (Excel)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.UserServiceExcelProvider#deleteUserProvider")
    void testDeleteUser_FromExcel(String testName, String userIdToDelete, String expectedError) {
        String actualResult = "PASSED";
        String actualMessage = "";

        // --- ARRANGE ---
        // (Không cần mock gì, vì mockito mặc định là void)

        // --- ACT & ASSERT ---
        try {
            userService.deleteUser(userIdToDelete);
            actualMessage = "Đã gọi Xóa (Delete)";
        } catch (Throwable e) {
            actualResult = "FAILED";
            actualMessage = e.getMessage().replaceAll("\n", " ");
        }
        testResults.add(
                new Object[] {testName, "UserID: " + userIdToDelete, "", expectedError, actualResult, actualMessage});
    }

    // --- Test 4: Đọc từ Sheet "GetMyInfo" ---
    @DisplayName("Test Get My Info (Excel)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.UserServiceExcelProvider#getMyInfoProvider")
    void testGetMyInfo_FromExcel(String testName, String loggedInUser, String expectedName, String expectedError) {
        String actualResult = "PASSED";
        String actualMessage = "";

        // --- ARRANGE ---
        // Giả lập (mock) tên user đang đăng nhập
        when(authentication.getName()).thenReturn(loggedInUser);

        // (Mock 'findByName' của code thật)
        if (expectedError != null && expectedError.equals("USER_NOT_EXISTED")) {
            lenient().when(userRepository.findByName(loggedInUser)).thenReturn(Optional.empty());
        } else {
            lenient().when(userRepository.findByName(loggedInUser)).thenReturn(Optional.of(sampleUser));
            lenient().when(userMapper.toUserResponse(sampleUser)).thenReturn(sampleUserResponse);
        }

        // --- ACT & ASSERT ---
        try {
            if (expectedError == null) {
                UserResponse response = userService.getMyInfo();
                assertNotNull(response);
                assertEquals(expectedName, response.getName()); // Kiểm tra tên trả về
                actualMessage = "Lấy thành công, tên: " + response.getName();
            } else {
                AppException exception = assertThrows(AppException.class, () -> userService.getMyInfo());
                assertEquals(expectedError, exception.getErrorCode().name());
                actualMessage = "Bắt được lỗi " + expectedError + " (ĐÚNG)";
            }
        } catch (Throwable e) {
            actualResult = "FAILED";
            actualMessage = e.getMessage().replaceAll("\n", " ");
        }
        testResults.add(new Object[] {
            testName, "User: " + loggedInUser, "Expected: " + expectedName, expectedError, actualResult, actualMessage
        });
    }

    // 5
    @DisplayName("Test Get User (Excel)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.UserServiceExcelProvider#getUserProvider")
    void testGetUser_FromExcel(
            String testName,
            String userId,
            String loggedInUser,
            String userRoles,
            String expectedUserName,
            String expectedError) {
        String actualResult = "PASSED";
        String actualMessage = "";

        // --- ARRANGE ---
        // Setup security context với user và roles
        setupSecurityContext(loggedInUser, userRoles.split(","));

        // Mock user data
        User requestedUser = User.builder()
                .id(userId)
                .name(expectedUserName)
                .email(expectedUserName + "@email.com")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(userId)
                .name(expectedUserName)
                .email(expectedUserName + "@email.com")
                .build();

        try {
            if ("USER_NOT_FOUND".equals(expectedError)) {
                when(userRepository.findById(userId)).thenReturn(Optional.empty());

                // Act & Assert
                RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.getUser(userId));
                assertTrue(exception.getMessage().contains("User not found"));
                actualMessage = "Bắt được lỗi User not found (ĐÚNG)";

            } else if ("ACCESS_DENIED".equals(expectedError)) {
                when(userRepository.findById(userId)).thenReturn(Optional.of(requestedUser));
                when(userMapper.toUserResponse(requestedUser)).thenReturn(userResponse);

                // Act & Assert - Should throw AccessDeniedException
                assertThrows(
                        org.springframework.security.access.AccessDeniedException.class,
                        () -> userService.getUser(userId));
                actualMessage = "Bắt được lỗi Access Denied (ĐÚNG)";

            } else {
                // SUCCESS case
                when(userRepository.findById(userId)).thenReturn(Optional.of(requestedUser));
                when(userMapper.toUserResponse(requestedUser)).thenReturn(userResponse);

                // Act
                UserResponse result = userService.getUser(userId);

                // Assert
                assertNotNull(result);
                assertEquals(expectedUserName, result.getName());
                actualMessage = "Lấy user thành công: " + result.getName();
            }

        } catch (Throwable e) {
            actualResult = "FAILED";
            actualMessage = e.getClass().getSimpleName() + ": " + e.getMessage().replaceAll("\n", " ");
        } finally {
            // Clear security context after test
            SecurityContextHolder.clearContext();
        }

        testResults.add(new Object[] {
            testName,
            "UserID: " + userId + ", LoggedIn: " + loggedInUser,
            "Roles: " + userRoles + ", Expected: " + expectedUserName,
            expectedError,
            actualResult,
            actualMessage
        });
    }

    private void setupSecurityContext(String username, String... roles) {
        List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities = java.util.Arrays.stream(
                        roles)
                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                .collect(java.util.stream.Collectors.toList());

        org.springframework.security.core.Authentication authentication =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        username, null, authorities);

        org.springframework.security.core.context.SecurityContext securityContext =
                SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // --- BƯỚC 5: Ghi kết quả TẤT CẢ các test ra Excel ---
    @AfterAll
    static void writeTestResultsToExcel() throws IOException {
        String filePath = "target/UserService-Test-Report.xlsx"; // Tên file output
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("UserService Test Results");

        // Tạo hàng Tiêu đề (Header)
        XSSFRow headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Test Case Name");
        headerRow.createCell(1).setCellValue("Input Data 1");
        headerRow.createCell(2).setCellValue("Input Data 2");
        headerRow.createCell(3).setCellValue("Expected Error");
        headerRow.createCell(4).setCellValue("Actual Result");
        headerRow.createCell(5).setCellValue("Message");

        // Ghi dữ liệu kết quả
        int rowNum = 1;
        for (Object[] result : testResults) {
            XSSFRow row = sheet.createRow(rowNum++);
            int colNum = 0;
            for (Object field : result) {
                if (field instanceof String) {
                    row.createCell(colNum++).setCellValue((String) field);
                } else {
                    row.createCell(colNum++).setCellValue("");
                }
            }
        }

        // Tự động điều chỉnh độ rộng cột
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }

        // Lưu file
        FileOutputStream outputStream = new FileOutputStream(filePath);
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();

        System.out.println("Báo cáo test đã được xuất ra file: " + filePath);
    }
}
