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

public class CCTVTest extends BaseTest {

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
        WebElement cctvLink =
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.nav-item[href='/cctv']")));

        // Hover qua menu
        Actions actions = new Actions(driver);
        actions.moveToElement(cctvLink).perform();

        // Chờ 500ms để hover animation hoàn tất
        Thread.sleep(1000);

        // Tìm lại element ngay trước click để đảm bảo không bị stale
        cctvLink = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a.nav-item[href='/cctv']")));

        // Click bằng Actions
        actions.click(cctvLink).perform();

        System.out.println("STEP 7: Chờ URL chứa /cctv");
        wait.until(ExpectedConditions.urlContains("/cctv"));

        System.out.println("STEP 8: Assert URL đúng");
        Assert.assertTrue(driver.getCurrentUrl().contains("/cctv"));

        System.out.println("TEST PASSED 🎉");
    }

    @Test(description = "Xem video CCTV dự án Cầu vượt Sông Hàn")
    public void CCTVvideo1() throws InterruptedException {

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
        WebElement cctvLink =
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.nav-item[href='/cctv']")));

        // Hover qua menu
        Actions actions = new Actions(driver);
        actions.moveToElement(cctvLink).perform();

        // Chờ 500ms để hover animation hoàn tất
        Thread.sleep(1000);

        // Tìm lại element ngay trước click để đảm bảo không bị stale
        cctvLink = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a.nav-item[href='/cctv']")));

        // Click bằng Actions
        actions.click(cctvLink).perform();

        System.out.println("STEP 7: Chờ URL chứa /cctv");
        wait.until(ExpectedConditions.urlContains("/cctv"));

        System.out.println("STEP 8: Assert URL đúng");
        Assert.assertTrue(driver.getCurrentUrl().contains("/cctv"));

        // ================= STEP 8: Chọn dự án trong dropdown =================
        System.out.println("STEP 8: Chọn dự án Cầu vượt Sông Hàn");
        WebElement selectProject =
                wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("select.customSelect")));

        Select projectDropdown = new Select(selectProject);
        projectDropdown.selectByVisibleText("Dự án Cầu vượt Sông Sài Gòn 3"); // text đúng như trong <option>

        System.out.println("STEP 8 PASSED: Đã chọn dự án thành công");

        // Bạn có thể tiếp tục các bước click nút xem video, assert video load,... ở đây
    }

    @Test(description = "Xem video CCTV dự án Cầu vượt Sông Hàn")
    public void CCTVvideo2() throws InterruptedException {

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
        System.out.println("STEP 5 PASSED: Nav-menu hiển thị");

        System.out.println("STEP 6: Hover + click menu CCTV");
        WebElement cctvLink =
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.nav-item[href='/cctv']")));
        Actions actions = new Actions(driver);
        actions.moveToElement(cctvLink).pause(Duration.ofMillis(1000)).click().perform();

        System.out.println("STEP 7: Chờ trang CCTV load xong");
        // Thay vì chỉ check URL, chờ dropdown dự án xuất hiện
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.select2 select.customSelect")));
        System.out.println("STEP 7 PASSED: Trang CCTV load xong");

        // ================= STEP 8: Chọn dự án =================
        System.out.println("STEP 8: Chọn dự án Cầu vượt Sông Hàn");
        WebElement projectSelect =
                wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div.select2 select.customSelect")));
        Select projectDropdown = new Select(projectSelect);
        projectDropdown.selectByVisibleText("Dự án Cầu vượt Sông Sài Gòn 3");
        System.out.println("STEP 8 PASSED: Đã chọn dự án");

        //        // ================= STEP 9: Chọn camera =================
        //        System.out.println("STEP 9: Chọn camera trong dự án");
        //        WebElement cameraSelect = wait.until(
        //                ExpectedConditions.elementToBeClickable(By.cssSelector("div.select1 select.customSelect"))
        //        );
        //        Select cameraDropdown = new Select(cameraSelect);
        //        cameraDropdown.selectByVisibleText("Kho Vật liệu (Giám sát PPE) (Khu vực kho vật tư, bãi tập kết
        // thép)");
        //        System.out.println("STEP 9 PASSED: Đã chọn camera");
        //
        //        // ================= STEP 10: Click Play Video =================
        //        System.out.println("STEP 10: Click nút xem video");
        //        WebElement playButton =
        // wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.play-video"))); // sửa selector
        // theo thực tế
        //        playButton.click();
        //        System.out.println("STEP 10 PASSED: Video đang chạy");
    }
}
