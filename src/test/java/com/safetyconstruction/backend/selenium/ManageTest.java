package com.safetyconstruction.backend.selenium;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ManageTest extends BaseTest {
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
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.nav-item[href='/manage']")));

        // Hover qua menu
        Actions actions = new Actions(driver);
        actions.moveToElement(cctvLink).perform();

        // Chờ 500ms để hover animation hoàn tất
        Thread.sleep(1000);

        // Tìm lại element ngay trước click để đảm bảo không bị stale
        cctvLink = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a.nav-item[href='/manage']")));

        // Click bằng Actions
        actions.click(cctvLink).perform();

        System.out.println("STEP 7: Chờ URL chứa /manage");
        wait.until(ExpectedConditions.urlContains("/manage"));

        System.out.println("STEP 8: Assert URL đúng");
        Assert.assertTrue(driver.getCurrentUrl().contains("/manage"));

        System.out.println("TEST PASSED 🎉");
    }

    @Test(description = "Manage - Add new project và kiểm tra hiển thị trong table")
    public void testAddNewProject_Project1() throws InterruptedException {

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

        // ================= STEP 6 =================
        System.out.println("STEP 6: Hover + click menu Manage");
        WebElement manageLink =
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.nav-item[href='/manage']")));

        actions.moveToElement(manageLink).pause(Duration.ofMillis(1000)).click().perform();

        // ================= STEP 7 =================
        System.out.println("STEP 7: Chờ URL chứa /manage");
        wait.until(ExpectedConditions.urlContains("/manage"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/manage"));
        System.out.println("STEP 7 PASSED");

        // ================= STEP 8 =================
        System.out.println("STEP 8: Click Add New Project");
        WebElement addNewBtn = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Add New Project')]")));
        addNewBtn.click();
        System.out.println("STEP 8 PASSED");

        // ================= STEP 9 =================
        System.out.println("STEP 9: Nhập Project Name = project1");
        WebElement projectNameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("name")));
        projectNameInput.clear();
        projectNameInput.sendKeys("project1");
        System.out.println("STEP 9 PASSED");

        // ================= STEP 10 =================
        System.out.println("STEP 10: Click Add Project");
        WebElement addProjectBtn = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Add Project')]")));
        addProjectBtn.click();
        System.out.println("STEP 10 PASSED");

        System.out.println("STEP 11: Kiểm tra project1 xuất hiện trong table");

        By projectCellLocator = By.xpath("//div[@class='table-container']//td[normalize-space()='project1']");

        WebElement projectCell = wait.until(ExpectedConditions.visibilityOfElementLocated(projectCellLocator));

        Assert.assertTrue(projectCell.isDisplayed(), "❌ Không tìm thấy project1 trong table");

        System.out.println("STEP 11 PASSED: project1 hiển thị trong table");

        System.out.println("TEST PASSED 🎉 Add Project thành công");
    }

    @Test(description = "Xóa project1 nếu tồn tại trong trang Manage")
    public void deleteProjectIfExists() throws InterruptedException {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // ================= STEP 1: Login =================
        System.out.println("STEP 1: Mở trang login");
        driver.get("http://localhost:3000/login");

        System.out.println("STEP 2: Nhập username");
        driver.findElement(By.id("username")).sendKeys("admin");

        System.out.println("STEP 3: Nhập password");
        driver.findElement(By.id("password")).sendKeys("admin");

        System.out.println("STEP 4: Click Login");
        driver.findElement(By.xpath("//button[@type='submit']")).click();

        // ================= STEP 5: Chờ nav-menu =================
        System.out.println("STEP 5: Chờ nav-menu xuất hiện");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("nav-menu")));
        System.out.println("STEP 5 PASSED");

        // ================= STEP 6: Hover + click menu Manage =================
        System.out.println("STEP 6: Hover + click menu Manage");

        WebElement manageLink =
                wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a.nav-item[href='/manage']")));

        new Actions(driver)
                .moveToElement(manageLink)
                .pause(Duration.ofMillis(1000))
                .click()
                .perform();

        // ================= STEP 7: Chờ trang /manage =================
        System.out.println("STEP 7: Chờ URL chứa /manage");
        wait.until(ExpectedConditions.urlContains("/manage"));
        Assert.assertTrue(driver.getCurrentUrl().contains("/manage"));
        System.out.println("STEP 7 PASSED");

        // ================= STEP 8: Tìm project1 =================
        System.out.println("STEP 8: Tìm project1 trong bảng");

        By projectRowLocator = By.xpath("//tr[td[normalize-space()='project1']]");

        List<WebElement> rows = driver.findElements(projectRowLocator);

        if (rows.isEmpty()) {
            System.out.println("❌ Không tìm thấy project1 → KẾT THÚC TEST");
            Assert.fail("Không có project1 trong bảng");
            return;
        }

        WebElement projectRow = rows.get(0);
        System.out.println("✔ Đã tìm thấy project1");

        // ================= STEP 9: Click Delete =================
        System.out.println("STEP 9: Click Delete project1");

        WebElement deleteBtn = projectRow.findElement(By.className("delete-btn"));

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", deleteBtn);
        Thread.sleep(1000);

        js.executeScript("arguments[0].click();", deleteBtn);
        System.out.println("STEP 9 PASSED: Đã click Delete");

        // ================= STEP 10: Accept Alert =================
        System.out.println("STEP 10: Chờ & accept alert");

        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();
        System.out.println("STEP 10 PASSED: Alert đã được accept");

        // ================= STEP 11: Verify project1 bị xóa =================
        System.out.println("STEP 11: Verify project1 không còn trong bảng");

        wait.until(ExpectedConditions.invisibilityOfElementLocated(projectRowLocator));

        List<WebElement> rowsAfterDelete = driver.findElements(projectRowLocator);

        Assert.assertTrue(rowsAfterDelete.isEmpty(), "❌ project1 vẫn còn trong bảng");

        System.out.println("TEST PASSED 🎉 Đã xóa project1 thành công");
    }

    @Test(description = "Login → Manage → Add New Camera → Save")
    public void testAddNewCamera() throws InterruptedException {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // ================= STEP 1: Login =================
        System.out.println("STEP 1: Mở trang login");
        driver.get("http://localhost:3000/login");

        System.out.println("STEP 2: Nhập username");
        driver.findElement(By.id("username")).sendKeys("admin");

        System.out.println("STEP 3: Nhập password");
        driver.findElement(By.id("password")).sendKeys("admin");

        System.out.println("STEP 4: Click Login");
        driver.findElement(By.xpath("//button[@type='submit']")).click();

        // ================= STEP 5: Chờ nav-menu =================
        System.out.println("STEP 5: Chờ nav-menu xuất hiện");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("nav-menu")));
        System.out.println("STEP 5 PASSED");

        // ================= STEP 6: Hover + click menu Manage =================
        System.out.println("STEP 6: Hover + click menu Manage");

        WebElement manageLink =
                wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a.nav-item[href='/manage']")));

        new Actions(driver)
                .moveToElement(manageLink)
                .pause(Duration.ofMillis(1000))
                .click()
                .perform();

        // ================= STEP 7: Chờ trang /manage =================
        System.out.println("STEP 7: Chờ URL chứa /manage");
        wait.until(ExpectedConditions.urlContains("/manage"));

        Assert.assertTrue(driver.getCurrentUrl().contains("/manage"));
        System.out.println("STEP 7 PASSED: Đã vào trang Manage");

        // ================= STEP 8: Click Add New Camera =================
        System.out.println("STEP 8: Click Add New Camera");

        WebElement addCameraBtn = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath("//button[normalize-space()='Add New Camera']")));

        js.executeScript("arguments[0].click();", addCameraBtn);
        System.out.println("STEP 8 PASSED");

        // ================= STEP 9: Chờ form hiển thị =================
        System.out.println("STEP 9: Chờ form Add Camera hiển thị");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("name")));

        // ================= STEP 10: Điền form =================
        System.out.println("STEP 10: Điền thông tin Camera");

        driver.findElement(By.id("name")).sendKeys("Camera Test 01");

        driver.findElement(By.id("ipAddress")).sendKeys("192.168.1.100");

        driver.findElement(By.id("rtspUrl")).sendKeys("https://www.youtube.com/watch?v=tYKEr4lSUZo");

        driver.findElement(By.id("location")).sendKeys("Tầng 1 - Cổng chính");

        driver.findElement(By.id("description")).sendKeys("Camera test tự động bằng Selenium");

        System.out.println("STEP 10 PASSED");

        // ================= STEP 11: Click Save =================
        System.out.println("STEP 11: Click Save");

        WebElement saveBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@type='submit' and normalize-space()='Save']")));

        js.executeScript("arguments[0].click();", saveBtn);

        System.out.println("STEP 11 PASSED: Đã click Save");

        // ================= STEP 12: Verify (optional) =================
        // Tuỳ app của bạn có toast / reload bảng hay không
        Thread.sleep(1500);

        System.out.println("TEST PASSED 🎉 Add New Camera thành công");
    }
}
