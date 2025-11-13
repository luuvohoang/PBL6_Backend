package com.safetyconstruction.backend.selenium;

import java.time.Duration;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import io.github.bonigarcia.wdm.WebDriverManager;

public class BaseTest {

    protected WebDriver driver;

    @BeforeMethod
    public void setUp() {
        // Tự động tải và thiết lập ChromeDriver
        WebDriverManager.chromedriver().setup();

        // (Tùy chọn) Cấu hình Chrome
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized"); // Mở toàn màn hình
        options.addArguments("--disable-extensions");
        //        options.addArguments("--headless=new"); // CHẠY Ở CHẾ ĐỘ ẨN (không mở UI trình duyệt)
        // Bỏ dòng "--headless=new" nếu bạn MUỐN THẤY trình duyệt chạy

        // Khởi tạo WebDriver
        driver = new ChromeDriver(options);

        // Thiết lập Implicit Wait (Chờ ngầm)
        // Nếu không tìm thấy element, nó sẽ thử lại trong 10 giây
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    @AfterMethod
    public void tearDown() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
// Lấy đối tượng coverage từ trình duyệt
        Object coverageData = js.executeScript("return window.__coverage__;");
        // Đóng trình duyệt sau khi mỗi test hoàn thành
        if (driver != null) {
            driver.quit();
        }
    }
}
