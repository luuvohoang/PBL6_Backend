// File: src/test/java/com/safetyconstruction/backend/controller/UserControllerTest.java
package com.safetyconstruction.backend.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safetyconstruction.backend.dto.request.UserCreationRequest;
import com.safetyconstruction.backend.entity.Role;
import com.safetyconstruction.backend.entity.User;
import com.safetyconstruction.backend.exception.ErrorCode;
import com.safetyconstruction.backend.repository.RoleRepository;
import com.safetyconstruction.backend.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
@TestPropertySource(
        properties = {
            "spring.profiles.active=test", // VÔ HIỆU HÓA TestDataInitializer
            "spring.main.allow-bean-definition-overriding=true"
        })
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    // Danh sách tĩnh (static) để lưu kết quả
    private static List<Object[]> testResults = new ArrayList<>();

    @BeforeAll
    static void clearTestResults() {
        testResults.clear();
    }

    // Helper method để setup mock data cho các test
    private void setupCommonMocks() {
        // Mock role USER luôn tồn tại
        Role userRole = Role.builder().name("USER").build();
        lenient().when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));

        // Mock role ADMIN cho các test cần
        Role adminRole = Role.builder().name("ADMIN").build();
        lenient().when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
    }

    @DisplayName("Test Create User (Controller - Excel)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.UserControllerExcelProvider#createUserControllerProvider")
    void testCreateUser_FromExcel(
            String testName, String requestBodyJson, int expectedHttpStatus, String expectedErrorCode) {

        String actualResult = "PASSED";
        String actualMessage = "";

        try {
            // --- ARRANGE (Thiết lập Mock) ---
            setupCommonMocks();

            // Đọc JSON để biết 'name' và 'email' là gì
            UserCreationRequest request = objectMapper.readValue(requestBodyJson, UserCreationRequest.class);

            // Giả lập (mock) logic 'existsBy...' (cho Service)
            if (expectedErrorCode != null && expectedErrorCode.equals("USER_EXISTED")) {
                lenient().when(userRepository.existsByName(request.getName())).thenReturn(true);
            } else if (expectedErrorCode != null && expectedErrorCode.equals("INVALID_EMAIL")) {
                lenient().when(userRepository.existsByName(request.getName())).thenReturn(false);
                lenient().when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);
            } else {
                // (Bao gồm TC1 - Thành công, và TC2 - Lỗi Validation)
                lenient().when(userRepository.existsByName(request.getName())).thenReturn(false);
                lenient().when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

                // Mock user entity trả về khi save thành công
                User savedUser = User.builder()
                        .id("generated-id")
                        .name(request.getName())
                        .email(request.getEmail())
                        .build();
                lenient().when(userRepository.save(any(User.class))).thenReturn(savedUser);
            }

            // --- ACT (Hành động) ---
            ResultActions result = mockMvc.perform(
                    post("/api/users").contentType(MediaType.APPLICATION_JSON).content(requestBodyJson));

            // --- ASSERT (Kiểm chứng) ---
            result.andExpect(status().is(expectedHttpStatus));

            if (expectedErrorCode != null && !expectedErrorCode.isEmpty()) {
                ErrorCode ec = ErrorCode.valueOf(expectedErrorCode);
                result.andExpect(jsonPath("$.code", is(ec.getCode())));
                actualMessage = "Bắt được lỗi " + expectedErrorCode + " (ĐÚNG)";
            } else {
                actualMessage = "Tạo thành công (200 OK)";
            }

        } catch (Throwable e) {
            actualResult = "FAILED";
            actualMessage = e.getMessage();
        }

        // --- GHI LẠI KẾT QUẢ ---
        testResults.add(new Object[] {
            testName,
            requestBodyJson,
            String.valueOf(expectedHttpStatus),
            expectedErrorCode,
            actualResult,
            actualMessage
        });
    }

    @DisplayName("Test PUT /api/users/{userId} (Update User)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.UserControllerExcelProvider#updateUserControllerProvider")
    @WithMockUser
    void testUpdateUser_FromExcel(
            String testName,
            String userIdToUpdate,
            String requestBodyJson,
            int expectedHttpStatus,
            String expectedErrorCode)
            throws Exception {

        // --- ARRANGE ---
        setupCommonMocks();

        if (expectedErrorCode != null && expectedErrorCode.equals("USER_NOT_EXISTED")) {
            when(userRepository.findById(userIdToUpdate)).thenReturn(Optional.empty());
        } else {
            User existingUser = User.builder()
                    .id(userIdToUpdate)
                    .name("existingUser")
                    .email("existing@email.com")
                    .build();
            when(userRepository.findById(userIdToUpdate)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenReturn(existingUser);
        }

        // --- ACT ---
        ResultActions result = mockMvc.perform(put("/api/users/" + userIdToUpdate)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBodyJson));

        // --- ASSERT ---
        result.andExpect(status().is(expectedHttpStatus));

        if (expectedErrorCode != null && !expectedErrorCode.isEmpty()) {
            ErrorCode ec = ErrorCode.valueOf(expectedErrorCode);
            result.andExpect(jsonPath("$.code", is(ec.getCode())));
        }
    }

    // --- Test 3: GetUsers (Test Bảo mật) ---
    @Test
    @DisplayName("Test GET /api/users (Thành công - ADMIN)")
    @WithMockUser(roles = "ADMIN")
    void getUsers_AsAdmin_ShouldSucceed() throws Exception {
        // --- ARRANGE ---
        setupCommonMocks();

        User user1 = User.builder().id("1").name("user1").build();
        User user2 = User.builder().id("2").name("user2").build();
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        // --- ACT & ASSERT ---
        mockMvc.perform(get("/api/users")).andExpect(status().isOk()).andExpect(jsonPath("$.result.length()", is(2)));
    }

    @Test
    @DisplayName("Test GET /api/users (Thất bại - MANAGER)")
    @WithMockUser(roles = "MANAGER")
    void getUsers_AsManager_ShouldBeForbidden() throws Exception {
        // --- ACT & ASSERT ---
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is(ErrorCode.UNAUTHORIZED.getCode())));
    }

    // --- Test 4: GetMyInfo (Test Bảo mật) ---
    @Test
    @DisplayName("Test GET /api/users/myInfo (Thành công)")
    @WithMockUser(username = "test-manager")
    void getMyInfo_AsLoggedInUser_ShouldSucceed() throws Exception {
        // --- ARRANGE ---
        setupCommonMocks();

        User managerUser = User.builder()
                .id("uuid-manager")
                .name("test-manager")
                .email("manager@email.com")
                .build();
        when(userRepository.findByName("test-manager")).thenReturn(Optional.of(managerUser));

        // --- ACT & ASSERT ---
        mockMvc.perform(get("/api/users/myInfo")) // Sửa endpoint đúng
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name", is("test-manager")));
    }

    // --- Test 5: DeleteUser (Test Bảo mật) ---
    @Test
    @DisplayName("Test DELETE /api/users/{userId} (Thất bại - MANAGER)")
    @WithMockUser(roles = "MANAGER")
    void deleteUser_AsManager_ShouldBeForbidden() throws Exception {
        // --- ACT & ASSERT ---
        mockMvc.perform(delete("/api/users/uuid-123"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is(ErrorCode.UNAUTHORIZED.getCode())));
    }

    @Test
    @DisplayName("Test DELETE /api/users/{userId} (Thành công - ADMIN)")
    @WithMockUser(roles = "ADMIN")
    void deleteUser_AsAdmin_ShouldSucceed() throws Exception {
        // --- ARRANGE ---
        setupCommonMocks();

        // Mock user tồn tại
        User userToDelete = User.builder().id("uuid-123").name("userToDelete").build();
        when(userRepository.findById("uuid-123")).thenReturn(Optional.of(userToDelete));

        // --- ACT & ASSERT ---
        mockMvc.perform(delete("/api/users/uuid-123")).andExpect(status().isOk());
    }

    // --- GHI FILE EXCEL OUTPUT ---
    @AfterAll
    static void writeTestResultsToExcel() throws IOException {
        String filePath = "target/UserController-Test-Report.xlsx";
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("UserController Test Results");

        // Header
        XSSFRow headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Test Case Name");
        headerRow.createCell(1).setCellValue("Input JSON Body");
        headerRow.createCell(2).setCellValue("Expected HTTP Status");
        headerRow.createCell(3).setCellValue("Expected Error Code");
        headerRow.createCell(4).setCellValue("Actual Result");
        headerRow.createCell(5).setCellValue("Message/Response");

        // Rows
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
