package com.safetyconstruction.backend.selenium;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ManageUsersTest extends BaseTest { // Kế thừa từ BaseTest

    private WebDriverWait wait;
    private final String MANAGE_URL = "http://localhost:3000/manage";

    @BeforeMethod
    public void setupTest() {
        // Khởi tạo WebDriverWait cho mỗi test
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    /**
     * Hàm trợ giúp (helper) để đăng nhập VÀ chờ cho Dashboard tải xong
     */
    private void loginAs(String username, String password) {
        driver.get("http://localhost:3000/login");
        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.xpath("//button[@type='submit']")).click();

        // Chờ trang Dashboard (hoặc trang chính) tải xong
        // (Dùng lại locator từ LoginTest)
        By dashboardBoxTitleLocator = By.className("box-title");
        wait.until(ExpectedConditions.visibilityOfElementLocated(dashboardBoxTitleLocator));
    }

    private void assertFrontendError(String fieldId, String expectedError) {
        // Locator: Tìm div 'errorMessages' ngay bên cạnh input có 'fieldId'
        By errorLocator = By.xpath("//input[@id='" + fieldId + "']/following-sibling::div[@class='errorMessages']");

        try {
            WebElement errorElement = wait.until(ExpectedConditions.visibilityOfElementLocated(errorLocator));
            Assert.assertEquals(errorElement.getText(), expectedError);
        } catch (Exception e) {
            Assert.fail("Không tìm thấy thông báo lỗi frontend cho '" + fieldId + "' với nội dung: " + expectedError);
        }
    }

    private WebElement assertSnackbar(String severity) {

        // --- ĐÂY LÀ CODE SỬA LỖI ---
        By snackbarLocator;

        // Thay vì tìm bằng attribute [severity=...],
        // chúng ta tìm bằng class mà MUI (Alert variant="filled") tạo ra.
        if (severity.equals("success")) {
            // Tìm Alert role VÀ có class MuiAlert-filledSuccess
            snackbarLocator = By.cssSelector("div[role='alert'].MuiAlert-filledSuccess");
        } else {
            // SỬA LỖI: Tìm class 'standardError' thay vì 'filledError'
            snackbarLocator = By.cssSelector("div[role='alert'].MuiAlert-standardError");
        }
        // --- HẾT CODE SỬA LỖI ---

        try {
            WebElement snackbar = wait.until(ExpectedConditions.visibilityOfElementLocated(snackbarLocator));
            Assert.assertTrue(snackbar.isDisplayed(), "Snackbar " + severity + " không hiển thị.");
            return snackbar;

        } catch (Exception e) {
            // Cung cấp thông báo lỗi rõ ràng hơn, chỉ rõ locator đã dùng
            Assert.fail("Không tìm thấy Snackbar (Alert) với CSS Selector: " + snackbarLocator.toString());
            return null;
        }
    }
    /**
     * Hàm trợ giúp (helper) để điều hướng đến trang Manage
     * (Dùng link từ Navbar.jsx)
     */
    private void navigateToManagePage() {
        // Locator: <Link to="/manage" ...> (tức là thẻ <a> với href="/manage")
        By manageLinkLocator = By.xpath("//a[@href='/manage']");
        wait.until(ExpectedConditions.elementToBeClickable(manageLinkLocator)).click();

        // Chờ tiêu đề "Manage" của trang Manage.jsx xuất hiện
        By manageHeaderLocator = By.xpath("//h1[text()='Manage']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(manageHeaderLocator));
    }

    // --- TEST CASES ---

    /**
     * Test Case 1: Kiểm tra luồng Logout
     * (Dùng locator từ Navbar.jsx)
     */
    @Test(description = "Kiểm tra đăng nhập và đăng xuất thành công")
    public void testLoginAndLogoutFlow() {
        loginAs("admin", "admin"); // (Dùng tài khoản admin của bạn)

        // Tìm nút Logout
        // Locator: <button onClick={handleLogout} className="logout-btn">
        By logoutButtonLocator = By.className("logout-btn");
        WebElement logoutButton = wait.until(ExpectedConditions.elementToBeClickable(logoutButtonLocator));
        logoutButton.click();

        // Chờ chuyển hướng về trang Login
        // (Code Navbar.jsx dùng window.location.href, nên ta chờ URL thay đổi)
        wait.until(ExpectedConditions.urlContains("/login"));

        // Assert: Đảm bảo ô "username" của trang Login hiển thị
        Assert.assertTrue(
                driver.findElement(By.id("username")).isDisplayed(), "Không quay về trang Login sau khi Logout.");
    }

    /**
     * Test Case 2 (Bảo mật): User KHÔNG phải Admin không thấy Quản lý User
     * QUAN TRỌNG: Bạn cần một tài khoản không phải Admin (ví dụ: MANAGER) để chạy test này
     */
    @Test(description = "User (không phải Admin) không thể thấy phần Quản lý User")
    public void testNonAdminCannotSeeUserManagement() {
        // **THAY THẾ** bằng một tài khoản KHÔNG CÓ ROLE_ADMIN
        loginAs("tom", "12345678");

        navigateToManagePage();

        // Tìm kiếm phần "Manage Users"
        // Locator: <div className="manage-users">
        By userSectionLocator = By.xpath("//h2[text()='Manage Users']");
        List<WebElement> userSections = driver.findElements(userSectionLocator);

        // Assert: Đảm bảo KHÔNG tìm thấy (size = 0)
        Assert.assertEquals(userSections.size(), 0, "User (không phải Admin) vẫn thấy phần 'Manage Users'!");
    }

    /**
     * Test Case 3 (Hiển thị): Admin có thể thấy Quản lý User
     */
    @Test(description = "Admin có thể thấy phần Quản lý User")
    public void testAdminCanSeeUserManagement() {
        loginAs("admin", "admin"); // Dùng tài khoản Admin
        navigateToManagePage();

        // Chờ phần "Manage Users" xuất hiện
        By userSectionLocator = By.xpath("//h2[text()='Manage Users']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(userSectionLocator));

        // Assert: Đảm bảo tìm thấy
        Assert.assertTrue(
                driver.findElement(userSectionLocator).isDisplayed(), "Admin không thấy phần 'Manage Users'.");

        // Assert: Đảm bảo thấy nút "Add New User"
        // Locator: <button>Add New User</button> bên trong <div class="manage-users">
        By addUserButtonLocator = By.xpath("//div[contains(@class, 'manage-users')]//button[text()='Add New User']");
        Assert.assertTrue(
                driver.findElement(addUserButtonLocator).isDisplayed(), "Admin không thấy nút 'Add New User'.");
    }

    @Test(description = "Admin có thể thêm mới và sau đó xóa một user")
    public void testAddAndThenDeleteUser() {
        // --- Dữ liệu test động (để test luôn duy nhất) ---
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7); // Lấy số ngẫu nhiên
        String username = "test_user_" + timestamp;
        String email = "test_" + timestamp + "@example.com";
        String password = "password123";
        String roleToAssign = "USER"; // Giả sử 'ROLE_USER' có trong data-test.sql

        // === PHẦN 1: ĐĂNG NHẬP VÀ MỞ MODAL ===
        loginAs("admin", "admin");
        navigateToManagePage();

        By addUserButtonLocator = By.xpath("//div[contains(@class, 'manage-users')]//button[text()='Add New User']");
        wait.until(ExpectedConditions.elementToBeClickable(addUserButtonLocator))
                .click();

        // === PHẦN 2: ĐIỀN FORM VÀ SUBMIT ===
        // Chờ modal xuất hiện
        By modalContentLocator = By.className("modal-content");
        wait.until(ExpectedConditions.visibilityOfElementLocated(modalContentLocator));

        // Điền các trường (locators từ AddUserModal.jsx)
        driver.findElement(By.id("name")).sendKeys(username);
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);

        // Click vào label của checkbox Role (An toàn hơn click checkbox)
        // (Giả sử roleToAssign = "ROLE_USER")
        By roleLabelLocator = By.xpath("//label[@for='role-" + roleToAssign + "']");
        wait.until(ExpectedConditions.elementToBeClickable(roleLabelLocator)).click();

        // Nhấn nút "Add User" (nút submit bên trong modal)
        By submitButtonLocator = By.xpath("//div[@class='modal-content']//button[@type='submit']");
        wait.until(ExpectedConditions.elementToBeClickable(submitButtonLocator)).click();

        // === PHẦN 3: KIỂM TRA USER ĐÃ ĐƯỢC THÊM ===
        // Chờ modal biến mất
        wait.until(ExpectedConditions.invisibilityOfElementLocated(modalContentLocator));

        // Chờ user mới xuất hiện trong bảng (kiểm tra bằng username)
        By userRowCellLocator = By.xpath("//td[text()='" + username + "']");
        WebElement userCell = wait.until(ExpectedConditions.visibilityOfElementLocated(userRowCellLocator));
        Assert.assertTrue(userCell.isDisplayed(), "User mới không xuất hiện trong bảng.");

        // (Kiểm tra thêm) Kiểm tra email và role trong bảng cho chắc
        String emailInTable = driver.findElement(By.xpath("//td[text()='" + username + "']/following-sibling::td[1]"))
                .getText();
        String roleInTable = driver.findElement(By.xpath("//td[text()='" + username + "']/following-sibling::td[2]"))
                .getText();

        Assert.assertEquals(emailInTable, email, "Email của user mới không đúng.");
        Assert.assertEquals(roleInTable, roleToAssign, "Role của user mới không đúng."); // Giả sử chỉ chọn 1 role

        // === PHẦN 4: XÓA USER VỪA TẠO ===
        // Tìm nút Delete TRONG CÙNG HÀNG (row) của user đó
        By deleteButtonLocator =
                By.xpath("//td[text()='" + username + "']/ancestor::tr//button[contains(@class, 'delete-btn')]");
        driver.findElement(deleteButtonLocator).click();

        // (Lưu ý: Code của bạn không có confirm dialog, nên nó sẽ bị xóa ngay)

        // === PHẦN 5: KIỂM TRA USER ĐÃ BIẾN MẤT ===
        // Chờ cho hàng (row) đó biến mất khỏi DOM
        wait.until(ExpectedConditions.stalenessOf(userCell));

        // Assert: Đảm bảo user không còn trong bảng
        List<WebElement> deletedUser = driver.findElements(userRowCellLocator);
        Assert.assertEquals(deletedUser.size(), 0, "User '" + username + "' vẫn còn trong bảng sau khi xóa.");

        System.out.println("Test Case 'Add and Delete User' (TC4) PASS!");
    }

    private WebElement assertErrorSnackbarIsVisible() {
        // Tìm Alert role VÀ có class MuiAlert-filledError
        By snackbarLocator = By.cssSelector("div[role='alert'].MuiAlert-filledError");

        try {
            WebElement snackbar = wait.until(ExpectedConditions.visibilityOfElementLocated(snackbarLocator));
            Assert.assertTrue(snackbar.isDisplayed(), "Snackbar lỗi 'error' không hiển thị.");
            System.out.println("LOG: Đã tìm thấy Snackbar lỗi: " + snackbar.getText());
            return snackbar;
        } catch (Exception e) {
            Assert.fail("Không tìm thấy Snackbar (Alert) với severity='error'");
            return null;
        }
    }

    /**
     * Test Case 6: Kiểm tra Lỗi Validation (Thêm User Trùng Email)
     * Sử dụng email của "admin" từ data-test.sql
     */
    @Test(description = "Kiểm tra lỗi khi thêm user có email đã tồn tại")
    public void testAddUser_EmailAlreadyExists() {
        // --- Dữ liệu test ---
        String username = "user_trung_email";
        String existingEmail = "admin@gmail.com"; // Email này từ data-test.sql
        String password = "password123";

        // === PHẦN 1: ĐĂNG NHẬP VÀ MỞ MODAL ===
        loginAs("admin", "admin");
        navigateToManagePage();

        By addUserButtonLocator = By.xpath("//div[contains(@class, 'manage-users')]//button[text()='Add New User']");
        wait.until(ExpectedConditions.elementToBeClickable(addUserButtonLocator))
                .click();

        // === PHẦN 2: ĐIỀN FORM VÀ SUBMIT ===
        By modalContentLocator = By.className("modal-content");
        wait.until(ExpectedConditions.visibilityOfElementLocated(modalContentLocator));

        driver.findElement(By.id("name")).sendKeys(username);
        driver.findElement(By.id("email")).sendKeys(existingEmail); // Dùng email đã tồn tại
        driver.findElement(By.id("password")).sendKeys(password);

        By roleLabelLocator = By.xpath("//label[@for='role-USER']");
        wait.until(ExpectedConditions.elementToBeClickable(roleLabelLocator)).click();

        By submitButtonLocator = By.xpath("//div[@class='modal-content']//button[@type='submit']");
        wait.until(ExpectedConditions.elementToBeClickable(submitButtonLocator)).click();

        // === PHẦN 3: KIỂM TRA LỖI ===
        // 1. Assert: Snackbar lỗi phải xuất hiện (từ API)
        WebElement errorSnackbar = assertSnackbar("error");

        // (Tùy chọn) Kiểm tra nội dung lỗi - Hãy thay đổi "Email already exists"
        // cho khớp với thông báo lỗi API của bạn
        System.out.println("THÔNG BÁO LỖI THỰC TẾ LÀ: " + errorSnackbar.getText());
        Assert.assertTrue(
                errorSnackbar.getText().contains("Email is not valid"), // Hoặc "Email already exists"
                "Nội dung thông báo lỗi không đúng.");

        // 2. Assert: Modal "Add User" VẪN PHẢI hiển thị
        Assert.assertTrue(
                driver.findElement(modalContentLocator).isDisplayed(),
                "Modal 'Add User' đã biến mất sau khi submit lỗi.");
    }

    /**
     * Test Case 7: Kiểm tra Lỗi Validation (Thêm User Bỏ trống Tên)
     * (Kiểm tra validation 'required' hoặc lỗi 400 từ backend)
     */
    /**
     * Test Case 7 (MỚI): Kiểm tra Validation Phía Trình duyệt
     * (Thay thế cho 'testAddUser_EmptyUsername' cũ)
     *
     * Test này kiểm tra xem các trường quan trọng (Username, Email, Password)
     * có thuộc tính 'required' của HTML5 hay không.
     */
    @Test(description = "Kiểm tra các trường 'Add User' bắt buộc (required)")
    public void testAddUser_FieldsAreRequired() {

        // === PHẦN 1: ĐĂNG NHẬP VÀ MỞ MODAL ===
        loginAs("admin", "admin"); // (Hàm helper của bạn)
        navigateToManagePage(); // (Hàm helper của bạn)

        By addUserButtonLocator = By.xpath("//div[contains(@class, 'manage-users')]//button[text()='Add New User']");
        wait.until(ExpectedConditions.elementToBeClickable(addUserButtonLocator))
                .click();

        // === PHẦN 2: CHỜ MODAL VÀ KIỂM TRA (ASSERT) ===
        By modalContentLocator = By.className("modal-content");
        wait.until(ExpectedConditions.visibilityOfElementLocated(modalContentLocator));

        // 1. Tìm trường Username
        WebElement usernameInput = driver.findElement(By.id("name"));
        // Lấy giá trị của thuộc tính 'required'
        String isUsernameRequired = usernameInput.getAttribute("required");

        // Assert: Đảm bảo thuộc tính 'required' tồn tại và là 'true'
        Assert.assertNotNull(isUsernameRequired, "Trường 'Username' (id='name') PHẢI CÓ thuộc tính 'required'.");
        Assert.assertEquals(
                isUsernameRequired, "true", "Trin 'Username' (id='name') required attribute should be 'true'.");

        // 2. Tìm trường Email
        WebElement emailInput = driver.findElement(By.id("email"));
        String isEmailRequired = emailInput.getAttribute("required");
        Assert.assertNotNull(isEmailRequired, "Trường 'Email' (id='email') PHẢI CÓ thuộc tính 'required'.");
        Assert.assertEquals(
                isEmailRequired, "true", "Trường 'Email' (id='email') required attribute should be 'true'.");

        // 3. Tìm trường Password
        WebElement passwordInput = driver.findElement(By.id("password"));
        String isPasswordRequired = passwordInput.getAttribute("required");
        Assert.assertNotNull(isPasswordRequired, "Trường 'Password' (id='password') PHẢI CÓ thuộc tính 'required'.");
        Assert.assertEquals(
                isPasswordRequired, "true", "Trường 'Password' (id='password') required attribute should be 'true'.");

        System.out.println("Test Case 7 (Kiểm tra Required Fields) PASS!");
    }
}
