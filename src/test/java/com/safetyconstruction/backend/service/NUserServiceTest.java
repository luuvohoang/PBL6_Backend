package com.safetyconstruction.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
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
class NUserServiceTest {

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
    //    private static List<Object[]> testResults = new ArrayList<>();

    private static Map<String, List<Object[]>> allTestResults = new HashMap<>();

    protected static void addTestResult(String sheetName, Object[] resultData) {
        // Lấy (hoặc tạo mới) danh sách kết quả cho sheet này
        allTestResults.computeIfAbsent(sheetName, k -> new ArrayList<>()).add(resultData);
    }

    @BeforeAll
    static void clearTestResults() {
        // Xóa sạch kết quả của các lần chạy TRƯỚC ĐÓ
        allTestResults.clear();
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
        String actualResult = "Passed";
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
            actualResult = "Failed";
            actualMessage = e.getMessage().replaceAll("\n", " ");
        }
        addTestResult("CreateUser", new Object[] {
            testName, request.getName(), request.getEmail(), expectedError, actualResult, actualMessage
        });
    }

    // --- Test 2: Đọc từ Sheet "UpdateUser" ---
    @DisplayName("Test Update User (Excel)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.UserServiceExcelProvider#updateUserProvider")
    void testUpdateUser_FromExcel(
            String testName, String userIdToUpdate, UserUpdateRequest request, String expectedError) {
        String actualResult = "Passed";
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
            actualResult = "Failed";
            actualMessage = e.getMessage().replaceAll("\n", " ");
        }
        // (Giả sử tên sheet của bạn là "UpdateUser")
        addTestResult("UpdateUser", new Object[] {
            testName, "UserID: " + userIdToUpdate, request.getEmail(), expectedError, actualResult, actualMessage
        });
    }

    // --- Test 3: Đọc từ Sheet "DeleteUser" ---
    @DisplayName("Test Delete User (Excel)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.UserServiceExcelProvider#deleteUserProvider")
    void testDeleteUser_FromExcel(String testName, String userIdToDelete, String expectedError) {
        String actualResult = "Passed";
        String actualMessage = "";

        // --- ARRANGE ---
        // (Không cần mock gì, vì mockito mặc định là void)

        // --- ACT & ASSERT ---
        try {
            userService.deleteUser(userIdToDelete);
            actualMessage = "Đã gọi Xóa (Delete)";
        } catch (Throwable e) {
            actualResult = "Failed";
            actualMessage = e.getMessage().replaceAll("\n", " ");
        }
        // (Giả sử tên sheet của bạn là "DeleteUser")
        addTestResult(
                "DeleteUser",
                new Object[] {testName, "UserID: " + userIdToDelete, "", expectedError, actualResult, actualMessage});
    }

    // --- Test 4: Đọc từ Sheet "GetMyInfo" ---
    @DisplayName("Test Get My Info (Excel)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("com.safetyconstruction.backend.util.UserServiceExcelProvider#getMyInfoProvider")
    void testGetMyInfo_FromExcel(String testName, String loggedInUser, String expectedName, String expectedError) {
        String actualResult = "Passed";
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
            actualResult = "Failed";
            actualMessage = e.getMessage().replaceAll("\n", " ");
        }
        // (Giả sử tên sheet của bạn là "GetMyInfo")
        addTestResult("GetMyInfo", new Object[] {
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
        String actualResult = "Passed";
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
            actualResult = "Failed";
            actualMessage = e.getClass().getSimpleName() + ": " + e.getMessage().replaceAll("\n", " ");
        } finally {
            // Clear security context after test
            SecurityContextHolder.clearContext();
        }

        // (Giả sử tên sheet của bạn là "GetUser")
        addTestResult("GetUser", new Object[] {
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

    //    import org.apache.poi.ss.usermodel.Cell; // (Hãy đảm bảo bạn đã import cái này)

    /**
     * Hàm trợ giúp MỚI để tìm chỉ số (index) của một cột dựa vào TÊN CỘT
     * (ví dụ: tìm "Actual Result" trong hàng 9).
     *
     * @param sheet Sheet để tìm kiếm.
     * @param headerRowIndex Chỉ số của hàng chứa tiêu đề (ví dụ: 9 cho hàng 10).
     * @param columnName Tên cột cần tìm (ví dụ: "Actual Result").
     * @return Chỉ số (index) của cột nếu tìm thấy, ngược lại trả về -1.
     */
    private static int findColumnIndexByName(XSSFSheet sheet, int headerRowIndex, String columnName) {
        XSSFRow headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) {
            System.err.println("LỖI: Hàng tiêu đề (index " + headerRowIndex + ") không tồn tại trong sheet "
                    + sheet.getSheetName());
            return -1; // Không tìm thấy hàng
        }

        // Lặp qua tất cả các ô (cell) trong hàng tiêu đề
        for (Cell cell : headerRow) {
            if (cell != null && cell.getCellType() == CellType.STRING) {
                // So sánh giá trị (không phân biệt hoa/thường)
                if (cell.getStringCellValue().trim().equalsIgnoreCase(columnName)) {
                    return cell.getColumnIndex(); // Tìm thấy! Trả về chỉ số cột.
                }
            }
        }

        // Không tìm thấy cột
        return -1;
    }

    // --- BƯỚC 5: Ghi kết quả TẤT CẢ các test ra Excel ---
    @AfterAll
    static void writeTestResultsToExcel() throws IOException {

        // --- BƯỚC 1: ĐỌC FILE TEMPLATE ---
        String templatePath = "src/test/resources/excel/User/Test_Case_Template.xlsx";
        FileInputStream inputStream = new FileInputStream(new File(templatePath));
        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        inputStream.close();

        // --- BƯỚC 2: ĐỊNH NGHĨA CÁC HẰNG SỐ (DỰA TRÊN YÊU CẦU CỦA BẠN) ---
        // Hàng 10 (index 9) là hàng chứa tên cột
        final int HEADER_ROW_INDEX = 9;

        // Tên cột chứa Test Case ID (Giả sử dựa trên hình của bạn là cột C)
        final String TEST_CASE_ID_COLUMN_NAME = "Test Case ID"; // <-- Sửa nếu tên này sai

        // Tên cột để ghi kết quả (Bạn muốn ghi vào Cột F, tên là gì? Tôi sẽ giả sử là "Actual Result")
        final String ACTUAL_RESULT_COLUMN_NAME = "Round 1"; // <-- Sửa nếu tên này sai

        // --- BƯỚC 3: LẶP QUA TỪNG SHEET CÓ KẾT QUẢ ---
        for (Map.Entry<String, List<Object[]>> entry : allTestResults.entrySet()) {

            String sheetName = entry.getKey();
            List<Object[]> sheetResults = entry.getValue();
            XSSFSheet sheet = workbook.getSheet(sheetName);

            if (sheet == null) {
                System.out.println("CẢNH BÁO: Không tìm thấy sheet '" + sheetName + "'. Bỏ qua...");
                continue;
            }

            // === BƯỚC 4: TÌM CỘT ĐỘNG (DYNAMIC COLUMN FINDING) ===
            // 4.1. Tìm chỉ số (index) của cột 'Test Case'
            int idColumnIndex = findColumnIndexByName(sheet, HEADER_ROW_INDEX, TEST_CASE_ID_COLUMN_NAME);
            if (idColumnIndex == -1) {
                System.err.println("LỖI: Không tìm thấy cột '" + TEST_CASE_ID_COLUMN_NAME + "' trên sheet '" + sheetName
                        + "'. Bỏ qua sheet này.");
                continue; // Chuyển sang sheet tiếp theo
            }

            // 4.2. Tìm chỉ số (index) của cột 'Actual Result'
            int resultColumnIndex = findColumnIndexByName(sheet, HEADER_ROW_INDEX, ACTUAL_RESULT_COLUMN_NAME);
            if (resultColumnIndex == -1) {
                System.err.println("LỖI: Không tìm thấy cột '" + ACTUAL_RESULT_COLUMN_NAME + "' trên sheet '"
                        + sheetName + "'. Bỏ qua sheet này.");
                continue;
            }

            System.out.println("Đang ghi sheet: " + sheetName + ". Dùng cột ID: "
                    + idColumnIndex + ", cột Kết quả: "
                    + resultColumnIndex);

            // --- BƯỚC 5: LẶP QUA KẾT QUẢ CỦA SHEET HIỆN TẠI ---
            for (Object[] result : sheetResults) {
                if (result[0] == null || result[4] == null) continue;

                String testCaseId = (String) result[0];
                String message = (String) result[4];

                // 5.1. Tìm hàng (row) khớp (dùng hàm đã sửa)
                XSSFRow row = findRowByTestCaseId(sheet, idColumnIndex, HEADER_ROW_INDEX, testCaseId);

                // 5.2. Ghi kết quả vào cột 'resultColumnIndex' (đã tìm động)
                if (row != null) {
                    XSSFCell cell = row.getCell(resultColumnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    cell.setCellValue(message);
                } else {
                    System.out.println(
                            "Không tìm thấy hàng cho Test Case ID: " + testCaseId + " trên sheet " + sheetName);
                }
            }
        } // Kết thúc vòng lặp qua các sheet

        System.out.println("Bắt đầu tính toán lại công thức (chạy 3 lượt)...");

        // 1. Tạo evaluator (trình đánh giá)
        XSSFFormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

        // 2. Xóa sạch bộ đệm (cache) CŨ (Chỉ cần làm một lần)
        evaluator.clearAllCachedResultValues();

        // --- SỬA LỖI: LẶP LẠI TOÀN BỘ QUÁ TRÌNH 3 LẦN ---
        // Điều này để xử lý các chuỗi phụ thuộc (ví dụ: SUM phụ thuộc vào COUNTIF)
        final int MAX_EVALUATION_PASSES = 3;

        for (int pass = 1; pass <= MAX_EVALUATION_PASSES; pass++) {
            System.out.println("--- Đang thực hiện tính toán Lượt " + pass + "/" + MAX_EVALUATION_PASSES + " ---");

            // 3. Lặp thủ công qua MỌI SHEET trong workbook
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                XSSFSheet sheet = workbook.getSheetAt(i);

                // 4. Lặp qua MỌI HÀNG (row) trong sheet
                for (Row row : sheet) {

                    // 5. Lặp qua MỌI Ô (cell) trong hàng
                    for (Cell cell : row) {

                        // 6. CHỈ KIỂM TRA NẾU NÓ LÀ CÔNG THỨC
                        if (cell != null && cell.getCellType() == CellType.FORMULA) {

                            // 7. Bọc trong try...catch cho TỪNG Ô
                            try {
                                // Ép tính toán lại ô CỤ THỂ này
                                evaluator.evaluateFormulaCell(cell);
                            } catch (Exception e) {
                                // (Chỉ in ra nếu đây là lượt cuối cùng, nếu không sẽ rất ồn ào)
                                if (pass == MAX_EVALUATION_PASSES) {
                                    System.err.println("LỖI TÍNH TOÁN (Lượt cuối): "
                                            + sheet.getSheetName() + "!"
                                            + cell.getAddress().formatAsString()
                                            + ". Lỗi: " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        } // Kết thúc vòng lặp "Pass"
        System.out.println("Đã hoàn tất tính toán lại công thức.");

        // --- BƯỚC 7: LƯU FILE ---
        String outputPath = "target/UserService-Test-Report-FILLED.xlsx";
        FileOutputStream outputStream = new FileOutputStream(outputPath);
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();

        System.out.println("Báo cáo test đã được GHI VÀO TEMPLATE và lưu tại: " + outputPath);
    }

    // (Hàm findRowByTestCaseId(sheet, testCaseId) giữ nguyên như cũ)

    /**
     * Hàm trợ giúp để tìm hàng (row) dựa trên giá trị của một ô (Test Case ID).
     *
     * @param sheet Sheet để tìm kiếm.
     * @param testCaseId ID của test case (ví dụ: "FP_TC01").
     * @return Hàng (XSSFRow) nếu tìm thấy, ngược lại trả về null.
     */
    /**
     * Hàm trợ giúp (đã sửa) để tìm hàng (row) dựa trên Test Case ID.
     *
     * @param sheet Sheet để tìm kiếm.
     * @param idColumnIndex Chỉ số cột chứa Test Case ID (được tìm động).
     * @param headerRowIndex Chỉ số của hàng tiêu đề (để bắt đầu tìm TỪ DƯỚI nó).
     * @param testCaseId ID cần tìm (ví dụ: "CU_TC01").
     * @return Hàng (XSSFRow) nếu tìm thấy, ngược lại trả về null.
     */
    private static XSSFRow findRowByTestCaseId(
            XSSFSheet sheet, int idColumnIndex, int headerRowIndex, String testCaseId) {

        // Lặp qua tất cả các hàng, bắt đầu TỪ SAU hàng tiêu đề
        for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
            XSSFRow row = sheet.getRow(i);
            if (row == null) {
                continue;
            }

            // Lấy ô ở cột Test Case ID (đã tìm động)
            XSSFCell cell = row.getCell(idColumnIndex);

            // So sánh giá trị
            if (cell != null && cell.getCellType() == CellType.STRING) {
                if (cell.getStringCellValue().trim().equals(testCaseId)) {
                    return row; // Tìm thấy!
                }
            }
        }
        return null; // Không tìm thấy
    }
}
