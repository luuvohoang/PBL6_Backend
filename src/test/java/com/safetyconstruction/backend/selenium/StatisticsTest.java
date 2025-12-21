package com.safetyconstruction.backend.selenium;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

public class StatisticsTest extends BaseTest {
    @Test(description = "Đăng nhập thành công và điều hướng sang trang CCTV")
    public void testLoginAndNavigateToCCTV() throws InterruptedException {

        System.out.println("STEP 1: Mở trang login");
        driver.get("http://localhost:3000/login");

        System.out.println("STEP 2: Nhập username");
        driver.findElement(By.id("username")).sendKeys("admin");

        System.out.println("STEP 3: Nhập password");
        driver.findElement(By.id("password")).sendKeys("admin");

        System.out.println("STEP 4: Click Login");
        driver.findElement(By.xpath("//button[@type='submit']")).click();

        System.out.println("STEP 5: Chờ nav-menu xuất hiện");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("nav-menu")));
        System.out.println("STEP 5 PASSED: Nav-menu hiển thị");

        System.out.println("STEP 6: Hover + click menu CCTV");

        // Tìm lại element ngay trước hover để tránh StaleElementReferenceException
        WebElement cctvLink = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.nav-item[href='/statistics']")));

        // Hover qua menu
        Actions actions = new Actions(driver);
        actions.moveToElement(cctvLink).perform();

        // Chờ 500ms để hover animation hoàn tất
        Thread.sleep(500);

        // Tìm lại element ngay trước click để đảm bảo không bị stale
        cctvLink =
                wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a.nav-item[href='/statistics']")));

        // Click bằng Actions
        actions.click(cctvLink).perform();

        System.out.println("STEP 7: Chờ URL chứa /statistics");
        wait.until(ExpectedConditions.urlContains("/statistics"));

        System.out.println("STEP 8: Assert URL đúng");
        Assert.assertTrue(driver.getCurrentUrl().contains("/statistics"));

        System.out.println("TEST PASSED 🎉");
    }

    @Test(description = "Statistics - Chọn dự án Tòa tháp The Sky và Search (check canvas)")
    public void testStatisticsSearchProjectTheSky_CheckCanvas() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        Actions actions = new Actions(driver);

        System.out.println("STEP 1: Mở trang login");
        driver.get("http://localhost:3000/login");

        System.out.println("STEP 2: Nhập username");
        driver.findElement(By.id("username")).sendKeys("admin");

        System.out.println("STEP 3: Nhập password");
        driver.findElement(By.id("password")).sendKeys("admin");

        System.out.println("STEP 4: Click Login");
        driver.findElement(By.xpath("//button[@type='submit']")).click();

        System.out.println("STEP 5: Chờ nav-menu xuất hiện");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("nav-menu")));
        System.out.println("STEP 5 PASSED");

        System.out.println("STEP 6: Hover + click menu Statistics");
        WebElement statisticsLink = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.nav-item[href='/statistics']")));

        actions.moveToElement(statisticsLink)
                .pause(Duration.ofMillis(1000))
                .click()
                .perform();

        System.out.println("STEP 7: Chờ URL chứa /statistics");
        wait.until(ExpectedConditions.urlContains("/statistics"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/statistics"));
        System.out.println("STEP 7 PASSED: Đã vào Statistics");

        // ================= STEP 8 =================
        System.out.println("STEP 8: Chờ form search load");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("form.search-form")));
        System.out.println("STEP 8 PASSED");

        // ================= STEP 9 =================
        System.out.println("STEP 9: Chọn dự án Tòa tháp The Sky (Quận 2)");
        WebElement projectSelect =
                wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("form.search-form select")));

        Select projectDropdown = new Select(projectSelect);
        projectDropdown.selectByVisibleText("Dự án Tòa tháp The Sky (Quận 2)");
        System.out.println("STEP 9 PASSED");

        // ================= STEP 10 =================
        System.out.println("STEP 10: Click Search");
        WebElement searchButton =
                wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.search-button")));
        searchButton.click();
        System.out.println("STEP 10 PASSED");

        // ================= STEP 11: CHECK CANVAS =================
        System.out.println("STEP 11: Kiểm tra canvas hiển thị");

        WebElement canvas = wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("canvas")));

        Assert.assertTrue(canvas.isDisplayed(), "Canvas không hiển thị!");
        System.out.println("STEP 11 PASSED: Canvas hiển thị");

        System.out.println("TEST PASSED 🎉 Statistics render thành công");
    }
}
