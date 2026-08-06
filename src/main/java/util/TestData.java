package util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class TestData {
	private FileInputStream fis = null;
	private XSSFWorkbook workbook = null;
	private XSSFSheet sheet = null;
	private XSSFRow row = null;
	private XSSFCell cell = null;
	FileOutputStream fileOut = null;

	public TestData(String xlFilePath) throws IOException {
		fis = new FileInputStream(xlFilePath);
		workbook = new XSSFWorkbook(fis);

		fis.close();

	}

	private int getTCRow(String TCID) {
		int rowIndex = -1;
		sheet = workbook.getSheetAt(0);

		for (rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
			row = sheet.getRow(rowIndex);
			if (row != null) {
				Cell cell = row.getCell(0);
				// Found column and there is value in the cell.
				if (cell != null) {

					String cellValue = cell.getStringCellValue();

					if (cellValue.equalsIgnoreCase(TCID) == true) {
						System.out.println("found value= " + cellValue + "row number= " + rowIndex);
						break;
					}
				}
			}
		}

		return rowIndex;
	}

	public String getCellData(String colName, int rowNum) {
		try {
			int col_Num = -1;
			sheet = workbook.getSheetAt(0);

			// Null check for header row
			row = sheet.getRow(0);
			if (row == null) {
				return "Header row is missing in Excel";
			}

			for (int i = 0; i < row.getLastCellNum(); i++) {
				Cell headerCell = row.getCell(i);
				if (headerCell != null && headerCell.getCellType() == CellType.STRING) {
					if (headerCell.getStringCellValue().trim().equalsIgnoreCase(colName.trim())) {
						col_Num = i;
						break;
					}
				}
			}

			if (col_Num == -1) {
				return "Column '" + colName + "' not found in Excel";
			}

			row = sheet.getRow(rowNum);
			if (row == null) {
				return "Row " + rowNum + " does not exist in Excel";
			}

			cell = row.getCell(col_Num);
			if (cell == null) {
				return ""; // blank cell
			}

			switch (cell.getCellType()) {
			case STRING:
				return cell.getStringCellValue().trim();
			case NUMERIC:
			case FORMULA:
				String cellValue = String.valueOf(cell.getNumericCellValue());
				if (HSSFDateUtil.isCellDateFormatted(cell)) {
					DateFormat df = new SimpleDateFormat("dd/MM/yy");
					Date date = cell.getDateCellValue();
					cellValue = df.format(date);
				}
				return cellValue;
			case BLANK:
				return "";
			case BOOLEAN:
				return String.valueOf(cell.getBooleanCellValue());
			default:
				return "Unsupported cell type";
			}

		} catch (Exception e) {
			e.printStackTrace();
			return "Error reading row " + rowNum + ", column '" + colName + "'";
		}
	}

	public String getTestData(String TCID, String colName) {
		String TCData;

		int TCRow = getTCRow(TCID);
		if (TCRow != -1) {
			TCData = getCellData(colName, TCRow);

		} else {
			TCData = "TEST CASE ID NOT FOUND IN TCID COLUMN OF TEST DATA FILE";
		}

		return TCData;

	}

	public boolean writeTestData(String TCID, String colName, String value) {

		try {
			int id = getTCRow(TCID);
			int col_Num = -1;
			sheet = workbook.getSheetAt(0);
			row = sheet.getRow(0);
			for (int i = 0; i < row.getLastCellNum(); i++) {
				if (row.getCell(i).getStringCellValue().trim().equals(colName.trim()))
					col_Num = i;
			}

			row = sheet.getRow(id);
			cell = row.getCell(col_Num);

			cell.setCellValue(value);

			// Open the output stream here, write, and then close it.
			FileOutputStream fileOut = new FileOutputStream(
					System.getProperty("user.dir") + "\\TestData\\starBucksTestData.xlsx");
			workbook.write(fileOut);
			fileOut.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
