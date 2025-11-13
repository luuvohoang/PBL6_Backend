// File: src/test/java/com/safetyconstruction/backend/controller/UserControllerIntegrationTest.java
package com.safetyconstruction.backend.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.safetyconstruction.backend.entity.Role;
import com.safetyconstruction.backend.entity.User;
import com.safetyconstruction.backend.exception.ErrorCode;
import com.safetyconstruction.backend.repository.RoleRepository;
import com.safetyconstruction.backend.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional // Đảm bảo CSDL được rollback (khôi phục) sau mỗi test
// @ActiveProfiles("test") // Kích hoạt application-test.properties VÀ TestDataInitializer
public class UserControllerIntegrationTest {

    @Container
    static final MySQLContainer<?> MY_SQL_CONTAINER = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MY_SQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MY_SQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MY_SQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    // Biến lưu trữ ID động (dynamic)
    private static String dynamicTestUserId;
    private static String dynamicAdminId;
    private static String dynamicManagerId;

    // Danh sách tĩnh (static) để lưu kết quả
    private static List<Object[]> testResults = new ArrayList<>();

    @BeforeAll
    static void clearTestResults() {
        testResults.clear();
    }

    @BeforeEach
    void setUp() {
        // Xóa sạch CSDL (database) trước MỖI test
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Khởi tạo lại dữ liệu mồi
        initTestData();

        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Sửa lại: Hàm này tạo TẤT CẢ dữ liệu cần thiết VÀ lưu ID động
     */
    private void initTestData() {
        Role userRole = roleRepository.save(
                Role.builder().name("USER").description("Regular user").build());
        Role adminRole = roleRepository.save(
                Role.builder().name("ADMIN").description("Administrator").build());
        Role managerRole = roleRepository.save(
                Role.builder().name("MANAGER").description("Manager").build());

        // Dữ liệu cho TC Trùng Username (existing_user)
        userRepository.save(User.builder()
                .name("existing_user")
                .email("existing@email.com")
                .password(passwordEncoder.encode("password123"))
                .roles(new HashSet<>(List.of(userRole)))
                .build());

        // Dữ liệu cho TC Trùng Email (other_user)
        userRepository.save(User.builder()
                .name("other_user")
                .email("other_email@email.com") // Sửa: Email phải là duy nhất
                .password(passwordEncoder.encode("password123"))
                .roles(new HashSet<>(List.of(userRole)))
                .build());

        // Admin user
        User admin = userRepository.save(User.builder()
                .name("admin1")
                .email("admin@email.com")
                .password(passwordEncoder.encode("admin123"))
                .roles(new HashSet<>(List.of(adminRole)))
                .build());
        dynamicAdminId = admin.getId(); // Lưu ID động

        // User cho các test khác
        User testUser = userRepository.save(User.builder()
                .name("testuser")
                .email("test@email.com")
                .password(passwordEncoder.encode("password123"))
                .roles(new HashSet<>(List.of(userRole)))
                .build());
        dynamicTestUserId = testUser.getId(); // Lưu ID động ("uuid-123" cũ)

        // Manager user
        User manager = userRepository.save(User.builder()
                .name("manager")
                .email("manager@email.com")
                .password(passwordEncoder.encode("manager123"))
                .roles(new HashSet<>(List.of(managerRole)))
                .build());
        dynamicManagerId = manager.getId(); // Lưu ID động
    }

    /**
     * Hàm phụ trợ (helper) để thay thế ID cứng (hard-coded)
     */
    private String resolveDynamicId(String idKey) {
        if ("uuid-123".equals(idKey)) return dynamicTestUserId;
        if ("admin".equals(idKey)) return dynamicAdminId;
        if ("manager".equals(idKey)) return dynamicManagerId;
        return idKey; // Trả về chính nó (ví dụ: "uuid-999")
    }

    // ========== CREATE USER TESTS (TỪ EXCEL) ==========
    @DisplayName("Test POST /api/users (Create User)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.InterUserControllerExcelProvider#createUserControllerProvider")
    @WithMockUser(roles = "ADMIN") // Cần quyền Admin
    void testCreateUser_FromExcel(
            String testName, String requestBodyJson, int expectedHttpStatus, String expectedErrorCode) {
        String actualResult = "PASSED";
        String actualMessage = "";

        try {
            ResultActions result = mockMvc.perform(MockMvcRequestBuilders.post("/api/users")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBodyJson));

            result.andExpect(MockMvcResultMatchers.status().is(expectedHttpStatus));

            if (expectedErrorCode != null) {
                ErrorCode ec = ErrorCode.valueOf(expectedErrorCode);
                result.andExpect(MockMvcResultMatchers.jsonPath("$.code", is(ec.getCode())));
                actualMessage = "Bắt được lỗi " + expectedErrorCode + " (ĐÚNG)";
            } else {
                actualMessage = "Tạo thành công (200 OK)";
            }
        } catch (Throwable e) {
            actualResult = "FAILED";
            actualMessage = e.getMessage().replaceAll("\n", " ");
        }
        testResults.add(new Object[] {
            testName,
            requestBodyJson,
            String.valueOf(expectedHttpStatus),
            expectedErrorCode,
            actualResult,
            actualMessage
        });
    }

    // ========== UPDATE USER TESTS (TỪ EXCEL) ==========
    @DisplayName("Test PUT /api/users/{userId} (Update User)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.InterUserControllerExcelProvider#updateUserControllerProvider")
    @WithMockUser(roles = "ADMIN") // Cần quyền Admin
    void testUpdateUser_FromExcel(
            String testName,
            String userIdKey,
            String requestBodyJson,
            int expectedHttpStatus,
            String expectedErrorCode) {
        String actualResult = "PASSED";
        String actualMessage = "";

        // SỬA: Dùng ID động (dynamic)
        String userIdToUpdate = resolveDynamicId(userIdKey);

        try {
            ResultActions result = mockMvc.perform(MockMvcRequestBuilders.put("/api/users/{userId}", userIdToUpdate)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBodyJson));

            result.andExpect(MockMvcResultMatchers.status().is(expectedHttpStatus));

            if (expectedErrorCode != null) {
                ErrorCode ec = ErrorCode.valueOf(expectedErrorCode);
                result.andExpect(MockMvcResultMatchers.jsonPath("$.code", is(ec.getCode())));
                actualMessage = "Bắt được lỗi " + expectedErrorCode + " (ĐÚNG)";
            } else {
                actualMessage = "Cập nhật thành công (200 OK)";
            }
        } catch (Throwable e) {
            actualResult = "FAILED";
            actualMessage = e.getMessage().replaceAll("\n", " ");
        }
        testResults.add(new Object[] {
            testName, "UserID: " + userIdToUpdate, requestBodyJson, expectedErrorCode, actualResult, actualMessage
        });
    }

    // ========== GET USER BY ID TESTS (TỪ EXCEL) ==========
    @DisplayName("Test GET /api/users/{userId} (Get User)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.InterUserControllerExcelProvider#getUserProvider")
    void testGetUserById_FromExcel(
            String testName,
            String userIdKey,
            String loggedInUser,
            String loggedInRoles,
            int expectedHttpStatus,
            String expectedErrorCode) {
        String actualResult = "PASSED";
        String actualMessage = "";

        // SỬA: Dùng ID động (dynamic)
        String userIdToGet = resolveDynamicId(userIdKey);

        try {
            ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get("/api/users/{userId}", userIdToGet)
                    .with(csrf())
                    .with(user(loggedInUser).roles(loggedInRoles.split(","))));

            result.andExpect(MockMvcResultMatchers.status().is(expectedHttpStatus));

            if (expectedErrorCode != null) {
                ErrorCode ec = ErrorCode.valueOf(expectedErrorCode);
                result.andExpect(MockMvcResultMatchers.jsonPath("$.code", is(ec.getCode())));
                actualMessage = "Bắt được lỗi " + expectedErrorCode + " (ĐÚNG)";
            } else {
                actualMessage = "Lấy thành công (200 OK)";
            }
        } catch (Throwable e) {
            actualResult = "FAILED";
            actualMessage = e.getMessage().replaceAll("\n", " ");
        }
        testResults.add(new Object[] {
            testName, "User: " + loggedInUser, "Roles: " + loggedInRoles, expectedErrorCode, actualResult, actualMessage
        });
    }

    // ========== GET MY INFO TESTS (TỪ EXCEL) ==========
    @DisplayName("Test GET /api/users/myInfo (Get My Info)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.InterUserControllerExcelProvider#getMyInfoProvider")
    void testGetMyInfo_FromExcel(
            String testName,
            String loggedInUser,
            String loggedInRoles,
            int expectedHttpStatus,
            String expectedErrorCode) {
        String actualResult = "PASSED";
        String actualMessage = "";

        try {
            ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get("/api/users/myInfo")
                    .with(csrf())
                    .with(user(loggedInUser).roles(loggedInRoles.split(","))));

            result.andExpect(MockMvcResultMatchers.status().is(expectedHttpStatus));

            if (expectedErrorCode != null) {
                ErrorCode ec = ErrorCode.valueOf(expectedErrorCode);
                result.andExpect(MockMvcResultMatchers.jsonPath("$.code", is(ec.getCode())));
                actualMessage = "Bắt được lỗi " + expectedErrorCode + " (ĐÚNG)";
            } else {
                actualMessage = "Lấy thành công (200 OK)";
            }
        } catch (Throwable e) {
            actualResult = "FAILED";
            actualMessage = e.getMessage().replaceAll("\n", " ");
        }
        testResults.add(new Object[] {
            testName, "User: " + loggedInUser, "Roles: " + loggedInRoles, expectedErrorCode, actualResult, actualMessage
        });
    }

    // ========== GET ALL USERS TESTS (TỪ EXCEL) ==========
    @DisplayName("Test GET /api/users (Get All Users)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.InterUserControllerExcelProvider#getUsersProvider")
    void testGetUsers_FromExcel(
            String testName,
            String loggedInUser,
            String loggedInRoles,
            int expectedHttpStatus,
            String expectedErrorCode) {
        String actualResult = "PASSED";
        String actualMessage = "";

        try {
            ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get("/api/users")
                    .with(csrf())
                    .with(user(loggedInUser).roles(loggedInRoles.split(","))));

            result.andExpect(MockMvcResultMatchers.status().is(expectedHttpStatus));

            if (expectedErrorCode != null) {
                ErrorCode ec = ErrorCode.valueOf(expectedErrorCode);
                result.andExpect(MockMvcResultMatchers.jsonPath("$.code", is(ec.getCode())));
                actualMessage = "Bắt được lỗi " + expectedErrorCode + " (ĐÚNG)";
            } else {
                actualMessage = "Lấy thành công (200 OK)";
            }
        } catch (Throwable e) {
            actualResult = "FAILED";
            actualMessage = e.getMessage().replaceAll("\n", " ");
        }
        testResults.add(new Object[] {
            testName, "User: " + loggedInUser, "Roles: " + loggedInRoles, expectedErrorCode, actualResult, actualMessage
        });
    }

    // ========== DELETE USER TESTS (TỪ EXCEL) ==========
    @DisplayName("Test DELETE /api/users/{userId} (Delete User)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.InterUserControllerExcelProvider#deleteUserProvider")
    void testDeleteUser_FromExcel(
            String testName,
            String userIdKey,
            String loggedInUser,
            String loggedInRoles,
            int expectedHttpStatus,
            String expectedErrorCode) {
        String actualResult = "PASSED";
        String actualMessage = "";

        // SỬA: Dùng ID động (dynamic)
        String userIdToDelete = resolveDynamicId(userIdKey);

        try {
            ResultActions result = mockMvc.perform(MockMvcRequestBuilders.delete("/api/users/{userId}", userIdToDelete)
                    .with(csrf())
                    .with(user(loggedInUser).roles(loggedInRoles.split(","))));

            result.andExpect(MockMvcResultMatchers.status().is(expectedHttpStatus));

            if (expectedErrorCode != null) {
                ErrorCode ec = ErrorCode.valueOf(expectedErrorCode);
                result.andExpect(MockMvcResultMatchers.jsonPath("$.code", is(ec.getCode())));
                actualMessage = "Bắt được lỗi " + expectedErrorCode + " (ĐÚNG)";
            } else {
                actualMessage = "Xóa thành công (200 OK)";
            }
        } catch (Throwable e) {
            actualResult = "FAILED";
            actualMessage = e.getMessage().replaceAll("\n", " ");
        }
        testResults.add(new Object[] {
            testName,
            "UserID: " + userIdToDelete,
            "User: " + loggedInUser,
            expectedErrorCode,
            actualResult,
            actualMessage
        });
    }

    // --- (Hàm @AfterAll để ghi Excel giữ nguyên) ---
    @AfterAll
    static void writeTestResultsToExcel() throws IOException {
        String filePath = "target/UserController-IntegrationTest-Report.xlsx";
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Controller Test Results");

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Test Case Name");
        headerRow.createCell(1).setCellValue("Input Data 1");
        headerRow.createCell(2).setCellValue("Input Data 2");
        headerRow.createCell(3).setCellValue("Expected Error");
        headerRow.createCell(4).setCellValue("Actual Result");
        headerRow.createCell(5).setCellValue("Message/Response");

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

        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
        FileOutputStream outputStream = new FileOutputStream(filePath);
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
        System.out.println("Báo cáo test (Controller) đã được xuất ra file: " + filePath);
    }
}
