// File: src/test/java/com/safetyconstruction/backend/util/ExcelTestUtils.java
package com.safetyconstruction.backend.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;

public class ExcelTestUtils {

    /**
     * Hàm dùng chung để đọc giá trị (value) của ô (cell) một cách an toàn
     * và xử lý trường hợp "(để trống)".
     */
    public static String getStringCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.BLANK) {
            return null;
        }

        String value = "";
        if (cell.getCellType() == CellType.STRING) {
            value = cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            value = String.valueOf(cell.getNumericCellValue());
        } else {
            value = cell.toString();
        }

        if ("(để trống)".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return value;
    }
}
