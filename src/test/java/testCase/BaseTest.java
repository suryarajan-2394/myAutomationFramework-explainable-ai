package testCase;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import io.github.bonigarcia.wdm.WebDriverManager;
import pages.CartPage;
import pages.HomePage;
import pages.LoginPage;
import util.TestData;
import util.utility;

public class BaseTest {

	protected WebDriver driver;
	static Properties properties;
	protected static TestData testData;
	static utility utils;
	protected static String URL;
	LoginPage loginPage;
	HomePage homePage;
	CartPage cartPage;
	static ExtentSparkReporter extentSparkReporter;
	static ExtentReports extentReports;
	protected static ThreadLocal<ExtentTest> extentTestThread = new ThreadLocal<ExtentTest>();

	@BeforeSuite
	public void startReport() throws IOException {
		String currDir = System.getProperty("user.dir");
		testData = new TestData(currDir + "\\TestData\\testData.xlsx");
		String reportFolderPath = currDir + "//AutomationReports//TestAutomationReport.html";
		extentSparkReporter = new ExtentSparkReporter(reportFolderPath);
		extentReports = new ExtentReports();
		extentReports.attachReporter(extentSparkReporter);
		extentSparkReporter.config().setDocumentTitle("Test Automation Report");
		extentSparkReporter.config().setReportName("Test Report For Automation");
		extentSparkReporter.config().setTheme(Theme.STANDARD);
		extentSparkReporter.config().setTimeStampFormat("EEEE, MMMM dd, yyyy, hh:mm a '('zzz')'");
		extentSparkReporter.config().setEncoding("utf-8");
		extentSparkReporter.config().setTimelineEnabled(true);
		extentSparkReporter.config().enableOfflineMode(true);

		// Initialize utils
		utils = new utility();

		// Load properties
		String propertyPath = currDir + "//src//main//resources//Environment.properties";
		properties = utility.loadProperties(propertyPath);
		URL = properties.getProperty("applicationURL");
	}

	@Parameters("browser")
	@BeforeMethod
	public void setup(@Optional("chrome") String browser, Method method) {
		if (browser.equalsIgnoreCase("chrome")) {
			ChromeOptions options = new ChromeOptions();
			Map<String, Object> prefs = new HashMap<>();
			prefs.put("credentials_enable_service", false);
			prefs.put("profile.password_manager_enabled", false);

			options.setExperimentalOption("prefs", prefs);
			options.addArguments("--disable-save-password-bubble");
			options.addArguments("--no-default-browser-check");
			options.addArguments("--disable-infobars");
			options.addArguments("--disable-notifications");
			options.addArguments("--disable-popup-blocking");

			WebDriverManager.chromedriver().setup();
			driver = new ChromeDriver(options);

		} else if (browser.equalsIgnoreCase("firefox")) {
			WebDriverManager.firefoxdriver().setup();
			driver = new FirefoxDriver();
		} else {
			throw new IllegalArgumentException("Browser not supported: " + browser);
		}
		loginPage = new LoginPage(driver);
		homePage = new HomePage(driver);
		cartPage = new CartPage(driver);
		extentTestThread.set(extentReports.createTest(method.getName()));
	}

	@AfterMethod
	public void getResult(ITestResult result) throws IOException {
		ExtentTest test = extentTestThread.get();
		if (result.getStatus() == ITestResult.FAILURE) {
			test.log(Status.FAIL, "Overall Test Status: FAILED");
			test.log(Status.FAIL, result.getThrowable());
			test.addScreenCaptureFromPath(utils.getScreenshotNew(driver));
		} else if (result.getStatus() == ITestResult.SUCCESS) {
			test.log(Status.PASS, "Overall Test Status: PASSED");
			test.addScreenCaptureFromPath(utils.getScreenshotNew(driver));
		} else {
			test.log(Status.SKIP, "Overall Test Status: SKIPPED");
			test.addScreenCaptureFromPath(utils.getScreenshotNew(driver));
		}
		driver.quit();
	}

	@AfterSuite
	public void tearDown() {
		if (extentReports != null) {
			extentReports.flush();
		}
	}
}
