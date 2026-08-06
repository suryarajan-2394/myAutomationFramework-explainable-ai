package testCase;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.aventstack.extentreports.Status;

public class AdvancedTestCase extends BaseTest {
	
	@Test
	public void validateUserNameSession() {
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
			String sessionUserCookie = driver.manage().getCookieNamed("session-username").getValue();
			 System.out.println(sessionUserCookie);
			 Assert.assertTrue(sessionUserCookie.equalsIgnoreCase(testData.getTestData("1", "userName")));
			 
			 extentTestThread.get().log(Status.PASS, "The Session is validated successfully with user name logged in");

		}catch (Throwable t) {
			extentTestThread.get().log(Status.FAIL, "Error : " + t.getMessage());
			throw t;
		}
	}
	
	@Test
	public void GetTextOverride() throws InterruptedException {
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

			loginPage.verifyErrorMessageUsingCustomElement(testData.getTestData("2", "errorMessage"));
			extentTestThread.get().log(Status.PASS, "Error Message Validated for login successfully");
		} catch (Throwable t) {
			extentTestThread.get().log(Status.FAIL,
					"Error during validation of login error message: " + t.getMessage());
			throw t;
		}

	}

}
