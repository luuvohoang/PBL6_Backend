// File: src/test/java/com/safetyconstruction/backend/controller/UserControllerTest.java
package com.safetyconstruction.backend.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

@SpringBootTest // Tải toàn bộ ứng dụng
@AutoConfigureMockMvc // Tự động cấu hình MockMvc
@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc; // Dùng để gọi API "giả"

    @Autowired
    private ObjectMapper objectMapper; // Dùng để chuyển JSON <-> Object

    // --- Giả lập (Mock) LỚP REPOSITORY ---
    // (Chúng ta test Controller + Service, nhưng giả lập CSDL)
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

    @DisplayName("Test Create User (Controller - Excel)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.UserControllerExcelProvider#createUserControllerProvider")
    void testCreateUser_FromExcel(
            String testName, String requestBodyJson, int expectedHttpStatus, String expectedErrorCode) {

        String actualResult = "PASSED";
        String actualMessage = "";

        try {
            // --- ARRANGE (Thiết lập Mock) ---

            // 1. Giả lập (mock) Role 'USER' (cho Service)
            Role userRole = Role.builder().name("USER").build();
            lenient().when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));

            // 2. Đọc JSON để biết 'name' và 'email' là gì
            UserCreationRequest request = objectMapper.readValue(requestBodyJson, UserCreationRequest.class);

            // 3. Giả lập (mock) logic 'existsBy...' (cho Service)
            if (expectedErrorCode != null && expectedErrorCode.equals("USER_EXISTED")) {
                lenient().when(userRepository.existsByName(request.getName())).thenReturn(true);
            } else if (expectedErrorCode != null && expectedErrorCode.equals("INVALID_EMAIL")) {
                lenient().when(userRepository.existsByName(request.getName())).thenReturn(false);
                lenient().when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);
            } else {
                // (Bao gồm TC1 - Thành công, và TC2 - Lỗi Validation)
                lenient().when(userRepository.existsByName(request.getName())).thenReturn(false);
                lenient().when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
                lenient().when(userRepository.save(any(User.class))).thenReturn(new User());
            }

            // --- ACT (Hành động) ---
            // Thực hiện cuộc gọi API POST "giả"
            ResultActions result = mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBodyJson)); // Gửi JSON body

            // --- ASSERT (Kiểm chứng) ---
            // 1. Kiểm tra (check) HTTP Status
            result.andExpect(status().is(expectedHttpStatus));

            // 2. Kiểm tra (check) Error Code (nếu có)
            if (expectedErrorCode != null && !expectedErrorCode.isEmpty()) {
                // Kiểm tra xem 'code' trong JSON response có khớp không
                ErrorCode ec = ErrorCode.valueOf(expectedErrorCode);
                result.andExpect(jsonPath("$.code", is(ec.getCode())));
                actualMessage = "Bắt được lỗi " + expectedErrorCode + " (ĐÚNG)";
            } else {
                actualMessage = "Tạo thành công (200 OK)";
            }

        } catch (Throwable e) {
            // Nếu 'result.andExpect' (ở trên) thất bại
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
    // Giả lập đã đăng nhập (vì PUT /api/users/ không phải là public)
    void testUpdateUser_FromExcel(
            String testName,
            String userIdToUpdate,
            String requestBodyJson,
            int expectedHttpStatus,
            String expectedErrorCode)
            throws Exception {

        // --- ARRANGE ---
        if (expectedErrorCode != null && expectedErrorCode.equals("USER_NOT_EXISTED")) {
            when(userRepository.findById(userIdToUpdate)).thenReturn(Optional.empty());
        } else {
            // (TC1 & TC2)
            when(userRepository.findById(userIdToUpdate)).thenReturn(Optional.of(new User()));
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

    // --- Test 3: GetUsers (Test Bảo mật - Không dùng Excel) ---
    @Test
    @DisplayName("Test GET /api/users (Thành công - ADMIN)")
    @WithMockUser(roles = "ADMIN") // Giả lập user đăng nhập với vai trò ADMIN
    void getUsers_AsAdmin_ShouldSucceed() throws Exception {

        // --- ARRANGE ---
        when(userRepository.findAll()).thenReturn(List.of(new User(), new User()));

        // --- ACT & ASSERT ---
        mockMvc.perform(get("/api/users")).andExpect(status().isOk()).andExpect(jsonPath("$.result.length()", is(2)));
    }

    @Test
    @DisplayName("Test GET /api/users (Thất bại - MANAGER)")
    @WithMockUser(roles = "MANAGER") // Giả lập user đăng nhập với vai trò MANAGER
    void getUsers_AsManager_ShouldBeForbidden() throws Exception {

        // --- ACT & ASSERT ---
        // (Lưu ý: Chúng ta đang test @PreAuthorize("hasRole('ADMIN')") của Service)
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden()) // Mong đợi 403
                .andExpect(jsonPath("$.code", is(ErrorCode.UNAUTHORIZED.getCode())));
    }

    // --- Test 4: GetMyInfo (Test Bảo mật - Không dùng Excel) ---
    @Test
    @DisplayName("Test GET /api/users/myInfo (Thành công)")
    // Giả lập user 'test-manager' đã đăng nhập
    @WithMockUser(username = "test-manager")
    void getMyInfo_AsLoggedInUser_ShouldSucceed() throws Exception {

        // --- ARRANGE ---
        User managerUser =
                User.builder().id("uuid-manager").name("test-manager").build();
        when(userRepository.findByName("test-manager")).thenReturn(Optional.of(managerUser));

        // --- ACT & ASSERT ---
        mockMvc.perform(get("/api/users/myInfo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name", is("test-manager")));
    }

    // --- Test 5: DeleteUser (Test Bảo mật - Không dùng Excel) ---
    @Test
    @DisplayName("Test DELETE /api/users/{userId} (Thất bại - MANAGER)")
    @WithMockUser(roles = "MANAGER") // Giả lập MANAGER
    void deleteUser_AsManager_ShouldBeForbidden() throws Exception {

        // --- ACT & ASSERT ---
        mockMvc.perform(delete("/api/users/uuid-123"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is(ErrorCode.UNAUTHORIZED.getCode())));
    }

    // --- GHI FILE EXCEL OUTPUT ---
    @AfterAll
    static void writeTestResultsToExcel() throws IOException {
        String filePath = "target/UserController-Test-Report.xlsx";
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("CreateUser Results");

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
                row.createCell(colNum++).setCellValue((String) field);
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
