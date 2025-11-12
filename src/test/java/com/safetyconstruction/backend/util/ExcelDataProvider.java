// File: src/test/java/com/safetyconstruction/backend/util/ExcelDataProvider.java
package com.safetyconstruction.backend.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.params.provider.Arguments;

import com.safetyconstruction.backend.dto.request.UserCreationRequest;
import com.safetyconstruction.backend.dto.request.UserUpdateRequest;

public class ExcelDataProvider {

    private static final String FILE_PATH = "src/test/resources/excel/UserService/UserService-testData.xlsx";

    // --- Helper: Đọc ô (cell) và xử lý "(để trống)" ---
    private static String getStringCellValue(Cell cell) {
        if (cell == null) return null;
        String value = "";
        switch (cell.getCellType()) {
            case STRING:
                value = cell.getStringCellValue();
                break;
            case NUMERIC:
                value = String.valueOf(cell.getNumericCellValue());
                break;
            case BLANK:
                return null;
            default:
                value = cell.toString();
        }
        if ("(để trống)".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return value;
    }

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
            String rolesStr = getStringCellValue(row.getCell(4)); // "ADMIN,USER"
            String expectedError = getStringCellValue(row.getCell(5));

            // --- SỬA LỖI: Chuyển sang List<String> ---
            List<String> roles = (rolesStr == null) ? new ArrayList<>() : Arrays.asList(rolesStr.split(","));

            UserUpdateRequest request = UserUpdateRequest.builder()
                    .email(email)
                    .password(password)
                    .roles(roles)
                    .build(); // (DTO yêu cầu List)
            // --- HẾT SỬA ---

            streamBuilder.add(Arguments.of(testName, userId, request, expectedError));
        }
        workbook.close();
        fis.close();
        return streamBuilder.build();
    }

    // --- Nguồn 3: Đọc Sheet "DeleteUser" ---
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
    public static Stream<Arguments> createUserControllerProvider() throws IOException {
        String filePath = "src/test/resources/excel/UserService/UserController-testData.xlsx";
        FileInputStream fis = new FileInputStream(filePath);
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheet("CreateUser");

        Stream.Builder<Arguments> streamBuilder = Stream.builder();
        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) rowIterator.next(); // Bỏ qua tiêu đề

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            String testName = getStringCellValue(row.getCell(0));
            String requestBodyJson = getStringCellValue(row.getCell(1));
            // Đọc HTTP Status (là số)
            int expectedHttpStatus = (int) row.getCell(2).getNumericCellValue();
            String expectedErrorCode = getStringCellValue(row.getCell(3));

            streamBuilder.add(Arguments.of(testName, requestBodyJson, expectedHttpStatus, expectedErrorCode));
        }
        workbook.close();
        fis.close();
        return streamBuilder.build();
    }
}
