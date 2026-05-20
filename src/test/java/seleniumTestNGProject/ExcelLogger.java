package seleniumTestNGProject;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExcelLogger {

    private Workbook workbook;
    private Sheet sheet;
    private int rowNum;

    public ExcelLogger(String sheetName) {
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet(sheetName);

        // Header row
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Module");
        header.createCell(1).setCellValue("Field/Action");
        header.createCell(2).setCellValue("Error Message");

        rowNum = 1;
    }

    // Log a bug
    public void log(String module, String fieldOrAction, String message) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(module);
        row.createCell(1).setCellValue(fieldOrAction);
        row.createCell(2).setCellValue(message);
    }

    // Save Excel file
    public void save(String filePath) {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
            workbook.close();
            System.out.println("✅ Excel file saved at: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
