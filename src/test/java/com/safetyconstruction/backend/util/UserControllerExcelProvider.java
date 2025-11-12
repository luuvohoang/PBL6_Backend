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

public class UserControllerExcelProvider {

    private static final String FILE_PATH = "src/test/resources/excel/User/UserController-testData.xlsx";

    // --- Nguồn 1: Đọc Sheet "CreateUser" cho Controller ---
    public static Stream<Arguments> createUserControllerProvider() throws IOException {
        FileInputStream fis = new FileInputStream(FILE_PATH);
        XSSFWorkbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheet("CreateUser");

        Stream.Builder<Arguments> streamBuilder = Stream.builder();
        Iterator<Row> rowIterator = sheet.iterator();
        if (rowIterator.hasNext()) rowIterator.next(); // Bỏ qua tiêu đề

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            // Gọi hàm tiện ích (utility) dùng chung
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
}
