package com.safetyconstruction.backend.selenium;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RegisterTest extends BaseTest { // Vẫn kế thừa BaseTest

    private final String REGISTER_URL = "http://localhost:3000/register";
    private WebDriverWait wait;

    // Chạy trước MỖI @Test trong file này
    @BeforeMethod
    public void setupTest() {
        // Khởi tạo WebDriverWait cho mỗi test
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        // 1. Luôn bắt đầu bằng cách truy cập trang đăng ký
        driver.get(REGISTER_URL);
    }

    // --- Helpers (Các hàm trợ giúp) ---

    /**
     * Hàm trợ giúp để điền 4 trường trong form
     */
    private void fillForm(String username, String email, String password, String confirmPass) {
        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("confirmPassword")).sendKeys(confirmPass);
    }

    /**
     * Hàm trợ giúp để nhấn nút Sign Up
     */
    private void clickSubmit() {
        // Locator: <button type="submit">Sign Up</button>
        driver.findElement(By.xpath("//button[@type='submit']")).click();
    }

    /**
     * Hàm trợ giúp để kiểm tra (Assert) lỗi VALIDATION PHÍA FRONTEND
     * (Các lỗi <div class="errorMessages">)
     *
     * @param fieldId ID của input (ví dụ: "username")
     * @param expectedError Văn bản lỗi mong đợi
     */
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

    /**
     * Hàm trợ giúp để kiểm tra (Assert) lỗi hoặc thành công từ API
     * (Các lỗi <Snackbar> / <Alert>)
     *
     * @param severity "success" hoặc "error"
     */
    private WebElement assertSnackbar(String severity) {

        // --- ĐÂY LÀ CODE SỬA LỖI ---
        By snackbarLocator;

        // Thay vì tìm bằng attribute [severity=...],
        // chúng ta tìm bằng class mà MUI (Alert variant="filled") tạo ra.
        if (severity.equals("success")) {
            // Tìm Alert role VÀ có class MuiAlert-filledSuccess
            snackbarLocator = By.cssSelector("div[role='alert'].MuiAlert-filledSuccess");
        } else {
            // Tìm Alert role VÀ có class MuiAlert-filledError
            snackbarLocator = By.cssSelector("div[role='alert'].MuiAlert-filledError");
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

    // --- TEST CASES ---

    /**
     * Test Case 1: Đăng ký thành công
     * Kỹ thuật: Dùng timestamp để tạo username/email duy nhất MỖI LẦN CHẠY
     * để test không bao giờ thất bại vì "user đã tồn tại".
     */
    @Test(description = "Kiểm thử đăng ký thành công")
    public void testSuccessfulRegistration() {
        // Tạo một user duy nhất
        String uniqueUser = "testuser_" + System.currentTimeMillis();
        String uniqueEmail = uniqueUser + "@example.com";

        fillForm(uniqueUser, uniqueEmail, "password123", "password123");
        clickSubmit();

        // 1. Kiểm tra thông báo thành công
        assertSnackbar("success");

        // 2. Kiểm tra xem nó có chuyển hướng về trang login không
        // (Code của bạn có setTimeout 2 giây, nên ta chờ 3 giây)
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(3));
        longWait.until(ExpectedConditions.urlContains("/login"));

        Assert.assertTrue(
                driver.getCurrentUrl().contains("/login"), "Không chuyển hướng về trang Login sau khi đăng ký.");
    }

    // --- CÁC TEST CASE LỖI FRONTEND ---

    @Test(description = "Bỏ trống Username")
    public void testRegistration_EmptyUsername() {
        fillForm("", "test@example.com", "password123", "password123");
        clickSubmit();
        assertFrontendError("username", "Username is required");
    }

    @Test(description = "Bỏ trống Email")
    public void testRegistration_EmptyEmail() {
        fillForm("testuser", "", "password123", "password123");
        clickSubmit();
        assertFrontendError("email", "Email is required");
    }

    @Test(description = "Email sai định dạng")
    public void testRegistration_InvalidEmailFormat() {
        fillForm("testuser", "email-sai-dinh-dang", "password123", "password123");
        clickSubmit();
        assertFrontendError("email", "Invalid email format");
    }

    @Test(description = "Bỏ trống Password")
    public void testRegistration_EmptyPassword() {
        fillForm("testuser", "test@example.com", "", "password123");
        clickSubmit();
        assertFrontendError("password", "Password must be at least 6 characters");
    }

    @Test(description = "Mật khẩu yếu (dưới 6 ký tự)")
    public void testRegistration_WeakPassword() {
        fillForm("testuser", "test@example.com", "12345", "12345");
        clickSubmit();
        assertFrontendError("password", "Password must be at least 6 characters");
    }

    @Test(description = "Mật khẩu không khớp")
    public void testRegistration_PasswordMismatch() {
        fillForm("testuser", "test@example.com", "password123", "password_KHAC");
        clickSubmit();
        assertFrontendError("confirmPassword", "Passwords do not match");
    }

    // --- CÁC TEST CASE LỖI BACKEND ---
    // QUAN TRỌNG: Bạn cần thay "admin" và "admin@example.com"
    // bằng một username/email THỰC SỰ đã tồn tại trong DB của bạn.

    @Test(description = "Email đã tồn tại")
    public void testRegistration_EmailAlreadyExists() {
        // Giả sử 'admin@example.com' đã tồn tại trong DB của bạn
        fillForm("newUser1", "luuvohoang07082004@gmail.com", "password123", "password123");
        clickSubmit();

        // Lần này, ta kiểm tra Snackbar lỗi
        WebElement errorSnackbar = assertSnackbar("error");

        // THÊM DÒNG NÀY ĐỂ GỠ LỖI:
        System.out.println("THÔNG BÁO LỖI THỰC TẾ LÀ: " + errorSnackbar.getText());

        // DÒNG NÀY ĐANG GÂY LỖI:
        Assert.assertTrue(errorSnackbar.getText().contains("Email is not valid"), "Nội dung thông báo lỗi không đúng.");
    }

    @Test(description = "Username đã tồn tại")
    public void testRegistration_UsernameAlreadyExists() {
        // Giả sử 'admin' đã tồn tại trong DB của bạn
        fillForm("admin", "new-email@example.com", "password123", "password123");
        clickSubmit();

        // Lần này, ta kiểm tra Snackbar lỗi
        WebElement errorSnackbar = assertSnackbar("error");

        System.out.println("THÔNG BÁO LỖI THỰC TẾ LÀ: " + errorSnackbar.getText());

        Assert.assertTrue(errorSnackbar.getText().contains("User existed"), "Nội dung thông báo lỗi không đúng.");
    }
}
