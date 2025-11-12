// File: src/test/java/com/safetyconstruction/backend/util/UserServiceExcelProvider.java
package com.safetyconstruction.backend.util;

import static com.safetyconstruction.backend.util.ExcelTestUtils.getStringCellValue;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.params.provider.Arguments;

import com.safetyconstruction.backend.dto.request.UserCreationRequest;
import com.safetyconstruction.backend.dto.request.UserUpdateRequest;

// (Lưu ý: Tên file Excel này là file chúng ta đã thống nhất)
public class UserServiceExcelProvider {

    private static final String FILE_PATH = "src/test/resources/excel/User/UserService-testData.xlsx";

    // --- Nguồn 1: Đọc Sheet "CreateUser" ---
    public static Stream<Arguments> createUserProvider() throws IOException {
        FileInputStream fis = new FileInputStream(FILE_PATH);
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheet("CreateUser");

        Stream.Builder<Arguments> streamBuilder = Stream.builder();
        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) rowIterator.next(); // Bỏ qua tiêu đề

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            // Gọi hàm tiện ích (utility) dùng chung
            String testName = getStringCellValue(row.getCell(0));
            String username = getStringCellValue(row.getCell(1));
            String email = getStringCellValue(row.getCell(2));
            String password = getStringCellValue(row.getCell(3));
            String expectedError = getStringCellValue(row.getCell(4));

            UserCreationRequest request = UserCreationRequest.builder()
                    .name(username)
                    .email(email)
                    .password(password)
                    .build();
            streamBuilder.add(Arguments.of(testName, request, expectedError));
        }
        workbook.close();
        fis.close();
        return streamBuilder.build();
    }

    // --- Nguồn 2: Đọc Sheet "UpdateUser" ---
    public static Stream<Arguments> updateUserProvider() throws IOException {
        FileInputStream fis = new FileInputStream(FILE_PATH);
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheet("UpdateUser");

        Stream.Builder<Arguments> streamBuilder = Stream.builder();
        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) rowIterator.next(); // Bỏ qua tiêu đề

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            String testName = getStringCellValue(row.getCell(0));
            String userId = getStringCellValue(row.getCell(1));
            String email = getStringCellValue(row.getCell(2));
            String password = getStringCellValue(row.getCell(3));
            String rolesStr = getStringCellValue(row.getCell(4));
            String expectedError = getStringCellValue(row.getCell(5));

            List<String> roles = (rolesStr == null) ? new ArrayList<>() : Arrays.asList(rolesStr.split(","));
            UserUpdateRequest request = UserUpdateRequest.builder()
                    .email(email)
                    .password(password)
                    .roles(roles)
                    .build();
            streamBuilder.add(Arguments.of(testName, userId, request, expectedError));
        }
        workbook.close();
        fis.close();
        return streamBuilder.build();
    }

    public static Stream<Arguments> deleteUserProvider() throws IOException {
        FileInputStream fis = new FileInputStream(FILE_PATH);
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheet("DeleteUser");

        Stream.Builder<Arguments> streamBuilder = Stream.builder();
        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) rowIterator.next(); // Bỏ qua tiêu đề

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            String testName = getStringCellValue(row.getCell(0));
            String userId = getStringCellValue(row.getCell(1));
            String expectedError = getStringCellValue(row.getCell(2));
            streamBuilder.add(Arguments.of(testName, userId, expectedError));
        }
        workbook.close();
        fis.close();
        return streamBuilder.build();
    }

    // --- Nguồn 4: Đọc Sheet "GetMyInfo" ---
    public static Stream<Arguments> getMyInfoProvider() throws IOException {
        FileInputStream fis = new FileInputStream(FILE_PATH);
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheet("GetMyInfo");

        Stream.Builder<Arguments> streamBuilder = Stream.builder();
        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) rowIterator.next(); // Bỏ qua tiêu đề

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            String testName = getStringCellValue(row.getCell(0));
            String loggedInUser = getStringCellValue(row.getCell(1));
            String expectedName = getStringCellValue(row.getCell(2));
            String expectedError = getStringCellValue(row.getCell(3));
            streamBuilder.add(Arguments.of(testName, loggedInUser, expectedName, expectedError));
        }
        workbook.close();
        fis.close();
        return streamBuilder.build();
    }

    // 5
    // --- Nguồn 5: Đọc Sheet "GetUser" ---
    public static Stream<Arguments> getUserProvider() throws IOException {
        FileInputStream fis = new FileInputStream(FILE_PATH);
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheet("GetUser");

        Stream.Builder<Arguments> streamBuilder = Stream.builder();
        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) rowIterator.next(); // Bỏ qua tiêu đề

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            String testName = getStringCellValue(row.getCell(0));
            String userId = getStringCellValue(row.getCell(1));
            String loggedInUser = getStringCellValue(row.getCell(2));
            String userRoles = getStringCellValue(row.getCell(3));
            String expectedUserName = getStringCellValue(row.getCell(4));
            String expectedError = getStringCellValue(row.getCell(5));

            streamBuilder.add(Arguments.of(
                    testName,
                    userId,
                    loggedInUser,
                    userRoles,
                    expectedUserName,
                    expectedError.isEmpty() ? null : expectedError));
        }
        workbook.close();
        fis.close();
        return streamBuilder.build();
    }

    public static Stream<Arguments> createUserIntegrationProvider() throws IOException {
        return createUserProvider(); // Reuse the same data
    }

    public static Stream<Arguments> updateUserIntegrationProvider() throws IOException {
        return updateUserProvider(); // Reuse the same data
    }

    public static Stream<Arguments> deleteUserIntegrationProvider() throws IOException {
        return deleteUserProvider(); // Reuse the same data
    }

    public static Stream<Arguments> getMyInfoIntegrationProvider() throws IOException {
        return getMyInfoProvider(); // Reuse the same data
    }

    public static Stream<Arguments> getUserIntegrationProvider() throws IOException {
        return getUserProvider(); // Reuse the same data
    }
}
