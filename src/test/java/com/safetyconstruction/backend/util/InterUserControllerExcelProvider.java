// File: src/test/java/com/safetyconstruction/backend/util/UserControllerExcelProvider.java
package com.safetyconstruction.backend.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.params.provider.Arguments;

public class InterUserControllerExcelProvider {

    private static final String FILE_PATH = "src/test/resources/excel/User/UserController-IntegrationTestData.xlsx";

    // --- Nguồn 1: Đọc Sheet "CreateUser" ---
    public static Stream<Arguments> createUserControllerProvider() throws IOException {
        FileInputStream fis = new FileInputStream(FILE_PATH);
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheet("CreateUser");

        Stream.Builder<Arguments> streamBuilder = Stream.builder();
        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) rowIterator.next(); // Bỏ qua tiêu đề

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            String testName = ExcelTestUtils.getStringCellValue(row.getCell(0));
            String requestBodyJson = ExcelTestUtils.getStringCellValue(row.getCell(1));
            int expectedHttpStatus = (int) row.getCell(2).getNumericCellValue();
            String expectedErrorCode = ExcelTestUtils.getStringCellValue(row.getCell(3));

            streamBuilder.add(Arguments.of(testName, requestBodyJson, expectedHttpStatus, expectedErrorCode));
        }
        workbook.close();
        fis.close();
        return streamBuilder.build();
    }

    // --- Nguồn 2: Đọc Sheet "UpdateUser" ---
    public static Stream<Arguments> updateUserControllerProvider() throws IOException {
        FileInputStream fis = new FileInputStream(FILE_PATH);
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheet("UpdateUser");

        Stream.Builder<Arguments> streamBuilder = Stream.builder();
        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) rowIterator.next(); // Bỏ qua tiêu đề

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            String testName = ExcelTestUtils.getStringCellValue(row.getCell(0));
            String userId = ExcelTestUtils.getStringCellValue(row.getCell(1));
            String requestBodyJson = ExcelTestUtils.getStringCellValue(row.getCell(2));
            int expectedHttpStatus = (int) row.getCell(3).getNumericCellValue();
            String expectedErrorCode = ExcelTestUtils.getStringCellValue(row.getCell(4));

            streamBuilder.add(Arguments.of(testName, userId, requestBodyJson, expectedHttpStatus, expectedErrorCode));
        }
        workbook.close();
        fis.close();
        return streamBuilder.build();
    }

    // --- Nguồn 3: Đọc Sheet "GetUser" ---
    public static Stream<Arguments> getUserProvider() throws IOException {
        FileInputStream fis = new FileInputStream(FILE_PATH);
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheet("GetUser");

        Stream.Builder<Arguments> streamBuilder = Stream.builder();
        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) rowIterator.next(); // Bỏ qua tiêu đề

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            String testName = ExcelTestUtils.getStringCellValue(row.getCell(0));
            String userIdToGet = ExcelTestUtils.getStringCellValue(row.getCell(1));
            String loggedInUser = ExcelTestUtils.getStringCellValue(row.getCell(2));
            String loggedInRoles = ExcelTestUtils.getStringCellValue(row.getCell(3));
            int expectedHttpStatus = (int) row.getCell(4).getNumericCellValue();
            String expectedErrorCode = ExcelTestUtils.getStringCellValue(row.getCell(5));

            streamBuilder.add(Arguments.of(
                    testName, userIdToGet, loggedInUser, loggedInRoles, expectedHttpStatus, expectedErrorCode));
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
            String testName = ExcelTestUtils.getStringCellValue(row.getCell(0));
            String loggedInUser = ExcelTestUtils.getStringCellValue(row.getCell(1));
            String loggedInRoles = ExcelTestUtils.getStringCellValue(row.getCell(2));
            int expectedHttpStatus = (int) row.getCell(3).getNumericCellValue();
            String expectedErrorCode = ExcelTestUtils.getStringCellValue(row.getCell(4));

            streamBuilder.add(
                    Arguments.of(testName, loggedInUser, loggedInRoles, expectedHttpStatus, expectedErrorCode));
        }
        workbook.close();
        fis.close();
        return streamBuilder.build();
    }

    // --- Nguồn 5: Đọc Sheet "GetUsers" ---
    public static Stream<Arguments> getUsersProvider() throws IOException {
        FileInputStream fis = new FileInputStream(FILE_PATH);
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheet("GetUsers");

        Stream.Builder<Arguments> streamBuilder = Stream.builder();
        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) rowIterator.next(); // Bỏ qua tiêu đề

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            String testName = ExcelTestUtils.getStringCellValue(row.getCell(0));
            String loggedInUser = ExcelTestUtils.getStringCellValue(row.getCell(1));
            String loggedInRoles = ExcelTestUtils.getStringCellValue(row.getCell(2));
            int expectedHttpStatus = (int) row.getCell(3).getNumericCellValue();
            String expectedErrorCode = ExcelTestUtils.getStringCellValue(row.getCell(4));

            streamBuilder.add(
                    Arguments.of(testName, loggedInUser, loggedInRoles, expectedHttpStatus, expectedErrorCode));
        }
        workbook.close();
        fis.close();
        return streamBuilder.build();
    }

    // --- Nguồn 6: Đọc Sheet "DeleteUser" ---
    public static Stream<Arguments> deleteUserProvider() throws IOException {
        FileInputStream fis = new FileInputStream(FILE_PATH);
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheet("DeleteUser");

        Stream.Builder<Arguments> streamBuilder = Stream.builder();
        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) rowIterator.next(); // Bỏ qua tiêu đề

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            String testName = ExcelTestUtils.getStringCellValue(row.getCell(0));
            String userIdToDelete = ExcelTestUtils.getStringCellValue(row.getCell(1));
            String loggedInUser = ExcelTestUtils.getStringCellValue(row.getCell(2));
            String loggedInRoles = ExcelTestUtils.getStringCellValue(row.getCell(3));
            int expectedHttpStatus = (int) row.getCell(4).getNumericCellValue();
            String expectedErrorCode = ExcelTestUtils.getStringCellValue(row.getCell(5));

            streamBuilder.add(Arguments.of(
                    testName, userIdToDelete, loggedInUser, loggedInRoles, expectedHttpStatus, expectedErrorCode));
        }
        workbook.close();
        fis.close();
        return streamBuilder.build();
    }
}
