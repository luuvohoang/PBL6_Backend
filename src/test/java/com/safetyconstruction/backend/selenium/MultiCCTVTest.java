package com.safetyconstruction.backend.selenium;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

public class MultiCCTVTest extends BaseTest {
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
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.nav-item[href='/multi-cctv']")));

        // Hover qua menu
        Actions actions = new Actions(driver);
        actions.moveToElement(cctvLink).perform();

        // Chờ 500ms để hover animation hoàn tất
        Thread.sleep(500);

        // Tìm lại element ngay trước click để đảm bảo không bị stale
        cctvLink =
                wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a.nav-item[href='/multi-cctv']")));

        // Click bằng Actions
        actions.click(cctvLink).perform();

        System.out.println("STEP 7: Chờ URL chứa /multi-cctv");
        wait.until(ExpectedConditions.urlContains("/multi-cctv"));

        System.out.println("STEP 8: Assert URL đúng");
        Assert.assertTrue(driver.getCurrentUrl().contains("/multi-cctv"));

        System.out.println("TEST PASSED 🎉");
    }

    @Test(description = "Chọn dự án Cầu vượt Sông Sài Gòn 3 và kiểm tra header")
    public void testSelectProjectAndVerifyHeader() {

        System.out.println("STEP 1: Mở trang login");
        driver.get("http://localhost:3000/login");

        System.out.println("STEP 2: Nhập username");
        driver.findElement(By.id("username")).sendKeys("admin");

        System.out.println("STEP 3: Nhập password");
        driver.findElement(By.id("password")).sendKeys("admin");

        System.out.println("STEP 4: Click Login");
        driver.findElement(By.xpath("//button[@type='submit']")).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        System.out.println("STEP 5: Chờ nav-menu xuất hiện");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("nav-menu")));
        System.out.println("STEP 5 PASSED");

        System.out.println("STEP 6: Hover + click menu Multi CCTV");
        WebElement cctvLink = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.nav-item[href='/multi-cctv']")));

        new Actions(driver)
                .moveToElement(cctvLink)
                .pause(Duration.ofMillis(1000))
                .click()
                .perform();

        System.out.println("STEP 7: Chờ trang Multi CCTV load xong");
        wait.until(ExpectedConditions.urlContains("/multi-cctv"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("multicctv-page")));
        System.out.println("STEP 7 PASSED");

        // ================= STEP 8: Click dự án =================
        System.out.println("STEP 8: Click Dự án Cầu vượt Sông Sài Gòn 3");

        WebElement projectItem = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath(
                                "//div[contains(@class,'sidebar-multicctv-item')]//div[@class='title' and text()='Dự án Cầu vượt Sông Sài Gòn 3']")));

        projectItem.click();
        System.out.println("STEP 8 PASSED");

        // ================= STEP 9: Verify H1 =================
        System.out.println("STEP 9: Kiểm tra content-header H1");

        WebElement headerH1 = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'content-header')]//h1")));

        String actualTitle = headerH1.getText().trim();
        String expectedTitle = "DỰ ÁN CẦU VƯỢT SÔNG SÀI GÒN 3";

        System.out.println("H1 TEXT = " + actualTitle);
        Assert.assertEquals(actualTitle, expectedTitle);

        System.out.println("TEST PASSED 🎉");
    }
}
