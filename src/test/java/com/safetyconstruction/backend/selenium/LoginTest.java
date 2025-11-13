package com.safetyconstruction.backend.selenium;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest { // Kế thừa từ BaseTest

    @Test(description = "Kiểm thử luồng đăng nhập thành công")
    public void testSuccessfulLoginFlow() {

        // 1. Mở trình duyệt và truy cập trang Đăng nhập
        driver.get("http://localhost:3000/login");

        // 2. Tìm ô "username" và nhập "admin"
        // (Locator lấy từ Login.jsx: id="username")
        WebElement usernameField = driver.findElement(By.id("username"));
        usernameField.sendKeys("admin");

        // 3. Tìm ô "password" và nhập "admin123"
        // (Locator lấy từ Login.jsx: id="password")
        WebElement passwordField = driver.findElement(By.id("password"));
        passwordField.sendKeys("admin"); // Dùng mật khẩu của bạn ở đây

        // 4. Nhấn nút "Login"
        // (Locator lấy từ Login.jsx: type="submit")
        WebElement loginButton = driver.findElement(By.xpath("//button[@type='submit']"));
        loginButton.click();

        // 5. Chờ (Wait) cho đến khi trang Dashboard được tải
        // Chúng ta dùng "Explicit Wait" (Chờ tường minh)
        // Đây là cách tốt nhất để xử lý các trang React/SPA

        // (Locator lấy từ Dashboard.jsx: <span>Dashboard</span>)
        // 5. Chờ (Wait) cho đến khi trang Dashboard được tải
        // Chúng ta sẽ chờ <div class="box-title"> xuất hiện.
        // Locator này cũng chỉ xuất hiện SAU KHI loading spinner biến mất.
        By dashboardBoxTitleLocator = By.className("box-title");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20)); // Chờ tối đa 20s
        WebElement dashboardBoxTitle =
                wait.until(ExpectedConditions.visibilityOfElementLocated(dashboardBoxTitleLocator));

        // 6. Kiểm tra (Assert) rằng văn bản "Dashboard" thực sự hiển thị
        // Khi div cha đã xuất hiện, ta tìm <span> con bên trong nó
        WebElement dashboardSpan = dashboardBoxTitle.findElement(By.tagName("span"));

        Assert.assertTrue(dashboardSpan.isDisplayed(), "Tiêu đề Dashboard (span) không hiển thị.");

        // Dùng .trim() để loại bỏ các khoảng trắng thừa (nếu có)
        Assert.assertEquals(dashboardSpan.getText().trim(), "Dashboard", "Văn bản tiêu đề không chính xác.");
    }

    @Test(description = "Kiểm thử đăng nhập thất bại do sai mật khẩu")
    public void testFailedLogin_WrongPassword() {

        // 1. Mở trình duyệt và truy cập trang Đăng nhập
        driver.get("http://localhost:3000/login");

        // 2. Tìm ô "username" và nhập "admin" (Đúng)
        WebElement usernameField = driver.findElement(By.id("username"));
        usernameField.sendKeys("admin");

        // 3. Tìm ô "password" và nhập "wrongpassword123" (SAI)
        WebElement passwordField = driver.findElement(By.id("password"));
        passwordField.sendKeys("wrongpassword123");

        // 4. Nhấn nút "Login"
        WebElement loginButton = driver.findElement(By.xpath("//button[@type='submit']"));
        loginButton.click();

        // 5. Chờ (Wait) cho thông báo lỗi (Snackbar/Alert) xuất hiện
        // Dựa trên code Login.jsx (MUI Alert), nó sẽ có role="alert"
        // Locator MỚI (đáng tin cậy hơn)
        By errorAlertLocator = By.cssSelector("div[role='alert']");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10)); // Chờ tối đa 10s
        WebElement errorAlert = wait.until(ExpectedConditions.visibilityOfElementLocated(errorAlertLocator));

        // 6. Kiểm tra (Assert) rằng thông báo lỗi đã hiển thị
        Assert.assertTrue(errorAlert.isDisplayed(), "Thông báo lỗi (Snackbar) không hiển thị.");

        // (Kiểm tra thêm) Đảm bảo chúng ta vẫn ở trang login
        Assert.assertTrue(
                driver.getCurrentUrl().contains("/login"),
                "Trang web đã chuyển hướng khỏi trang Login một cách không mong muốn.");

        System.out.println("Test Case 2 (Đăng nhập thất bại) PASS!");
    }

    @Test(description = "Kiểm thử đăng nhập thất bại do sai tên đăng nhập")
    public void testFailedLogin_WrongUsername() {
        // Gọi phương thức trợ giúp
        performFailedLoginTest("wrong-user", "admin123");
    }

    /**
     * Test Case 4 (MỚI): Đăng nhập thất bại - Bỏ trống mật khẩu
     */
    @Test(description = "Kiểm thử đăng nhập thất bại do bỏ trống mật khẩu")
    public void testFailedLogin_EmptyPassword() {
        // Gọi phương thức trợ giúp
        performFailedLoginTest("admin", ""); // Gửi một chuỗi rỗng
    }

    /**
     * Test Case 5 (MỚI): Đăng nhập thất bại - Bỏ trống tên đăng nhập
     */
    @Test(description = "Kiểm thử đăng nhập thất bại do bỏ trống tên đăng nhập")
    public void testFailedLogin_EmptyUsername() {
        // Gọi phương thức trợ giúp
        performFailedLoginTest("", "admin123"); // Gửi một chuỗi rỗng
    }

    // --- PHƯƠNG THỨC TRỢ GIÚP (HELPER METHOD) ---

    /**
     * Phương thức này thực hiện một lần đăng nhập thất bại và kiểm tra
     * xem thông báo lỗi (Snackbar) có hiển thị hay không.
     *
     * @param username Tên đăng nhập để nhập
     * @param password Mật khẩu để nhập
     */
    private void performFailedLoginTest(String username, String password) {

        // 1. Mở trình duyệt và truy cập trang Đăng nhập
        driver.get("http://localhost:3000/login");

        // 2. Tìm và nhập thông tin (sendKeys("") là hợp lệ cho trường rỗng)
        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);

        // 3. Nhấn nút "Login"
        driver.findElement(By.xpath("//button[@type='submit']")).click();

        // 4. Chờ (Wait) cho thông báo lỗi (Snackbar/Alert) xuất hiện
        // Dùng locator đã sửa ở lần trước: div[role='alert']
        By errorAlertLocator = By.cssSelector("div[role='alert']");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement errorAlert = wait.until(ExpectedConditions.visibilityOfElementLocated(errorAlertLocator));

        // 5. Kiểm tra (Assert) rằng thông báo lỗi đã hiển thị
        Assert.assertTrue(
                errorAlert.isDisplayed(),
                "Thông báo lỗi (Snackbar) không hiển thị cho: user='" + username + "', pass='" + password + "'");

        // 6. (Kiểm tra thêm) Đảm bảo chúng ta vẫn ở trang login
        Assert.assertTrue(
                driver.getCurrentUrl().contains("/login"),
                "Trang web đã chuyển hướng khỏi trang Login một cách không mong muốn.");

        System.out.println("Test Case thất bại (user: '" + username + "') PASS!");
    }
}
