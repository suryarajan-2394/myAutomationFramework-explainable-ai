package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class utility {
	private static final DateTimeFormatter REPORT_TIME = DateTimeFormatter.ofPattern("dd-MM-uuuu_HH-mm-ss");
	private static String previousSecond = "";
	private static int sameSecond = 0;
	static WebDriver driver;
	static Properties properties;
	static InputStream input;
	static WebElement element;

	public static Properties loadProperties(String path) {
		try {
			input = new FileInputStream(path);
			properties = new Properties();
			properties.load(input);
			return properties;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return properties;
	}

	public static String getScreenshot(WebDriver driver) throws IOException {
		String screenshotFilePath;
		screenshotFilePath = System.getProperty("user.dir") + "//AutomationReports//" + utility.timeStamp() + ".png";
		File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
		FileUtils.copyFile(screenshotFile, new File(screenshotFilePath));
		return screenshotFilePath;
	}

	public static String getScreenshotNew(WebDriver driver) throws IOException {
		return getScreenshotNew(driver, timeStamp());
	}

	public static String getScreenshotNew(WebDriver driver, String stamp) throws IOException {
    // file name
    String fileName = stamp + ".png";

    // Base folder where Extent report resides
    String reportFolder = System.getProperty("user.dir") + File.separator + "AutomationReports";

    // Screenshots sub-folder (inside AutomationReports)
    File screenshotsDir = new File(reportFolder + File.separator + "Screenshots");
    if (!screenshotsDir.exists()) {
        screenshotsDir.mkdirs();
    }

    // Full absolute path where screenshot will be saved
    String screenshotFullPath = screenshotsDir.getAbsolutePath() + File.separator + fileName;

    // Take screenshot to temp file and copy to destination
    File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
    File destFile = new File(screenshotFullPath);
    org.apache.commons.io.FileUtils.copyFile(srcFile, destFile);

    // Return relative path *for HTML*: "Screenshots/<filename>.png"
    // Use forward slashes because HTML expects that
    return "Screenshots/" + fileName;
}


	public static synchronized String timeStamp() {
		String second = LocalDateTime.now().format(REPORT_TIME);
		if (!second.equals(previousSecond)) {
			previousSecond = second;
			sameSecond = 0;
		}
		int duplicate = sameSecond++;
		return duplicate == 0 ? second : second + "_" + duplicate;

	}
	

}
