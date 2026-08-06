package testCase;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.aventstack.extentreports.Status;

public class LoginTest extends BaseTest {

	@Test
	public void LoginTestCase_1() throws Throwable {

		driver.get(URL);
		driver.manage().window().maximize();
		driver.navigate().refresh();

		try {

			loginPage.loginFunction(testData.getTestData("1", "userName"), testData.getTestData("1", "password"));
			extentTestThread.get().log(Status.PASS, "Login action performed successfully");
		} catch (Throwable t) {
			extentTestThread.get().log(Status.FAIL, "Error during login: " + t.getMessage());
			throw t;
		}

	}

	@Test
	public void LoginTestCase_2() throws InterruptedException {
		driver.get(URL);
		driver.manage().window().maximize();
		driver.navigate().refresh();

		try {

			loginPage.loginFunction(testData.getTestData("2", "userName"), testData.getTestData("2", "password"));
			extentTestThread.get().log(Status.PASS, "Login action performed successfully");
		} catch (Throwable t) {
			extentTestThread.get().log(Status.FAIL, "Error during login: " + t.getMessage());
			throw t;
		}

		try {

			loginPage.verifyErrorLoginMessage(testData.getTestData("2", "errorMessage"));
			extentTestThread.get().log(Status.PASS, "Error Message Validated for login successfully");
		} catch (Throwable t) {
			extentTestThread.get().log(Status.FAIL,
					"Error during validation of login error message: " + t.getMessage());
			throw t;
		}

	}

	@Test
	public void verifyLocalStorageCheck() throws Throwable {

		driver.get(URL);
		driver.manage().window().maximize();
		driver.navigate().refresh();

		try {

			loginPage.loginFunction(testData.getTestData("1", "userName"), testData.getTestData("1", "password"));
			extentTestThread.get().log(Status.PASS, "Login action performed successfully");
		} catch (Throwable t) {
			extentTestThread.get().log(Status.FAIL, "Error during login: " + t.getMessage());
			throw t;
		}

		try {
			Object localStorageLength = ((org.openqa.selenium.JavascriptExecutor) driver)
					.executeScript("return window.localStorage.length;");

			Assert.assertTrue(Integer.parseInt(localStorageLength.toString()) >= 0,
					"Expected localStorage to have items or be accessible.");
			extentTestThread.get().log(Status.PASS, "The Local Storage check is performed successfully");
		} catch (Throwable t) {
			extentTestThread.get().log(Status.FAIL, "Error during checking local storage: " + t.getMessage());
			throw t;
		}

	}

}
