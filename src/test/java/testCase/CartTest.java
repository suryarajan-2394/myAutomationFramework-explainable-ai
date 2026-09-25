package testCase;

import org.testng.annotations.Test;

import com.aventstack.extentreports.Status;

public class CartTest extends BaseTest {

	@Test
	public void AddToCartTestCase() throws Throwable {

		driver.get(URL);
		driver.manage().window().maximize();
		driver.navigate().refresh();

		try {

			explainedStep("Log in", "Inventory page is displayed", () -> loginPage.loginFunction(testData.getTestData("3", "userName"), testData.getTestData("3", "password")));
			extentTestThread.get().log(Status.PASS, "Login action performed successfully");
		} catch (Throwable t) {
			extentTestThread.get().log(Status.FAIL, "Error during login: " + t.getMessage());
			throw t;
		}
		
		try {

			explainedStep("Add configured product", "Product is added to cart", () -> homePage.selectProduct(testData.getTestData("3", "productName")));
			extentTestThread.get().log(Status.PASS, "Product is selected and is added to Cart successfully");
		} catch (Throwable t) {
			extentTestThread.get().log(Status.FAIL, "Error during adding products to Cart: " + t.getMessage());
			throw t;
		}
		
		try {

			explainedStep("Open cart", "Cart page is displayed", () -> homePage.clickOnCart());
			extentTestThread.get().log(Status.PASS, "Cart Icon clicked successfully");
		} catch (Throwable t) {
			extentTestThread.get().log(Status.FAIL, "Error during clicking Cart: " + t.getMessage());
			throw t;
		}
		
		try {

			explainedStep("Validate cart product", "Configured product is present in cart", () -> cartPage.validateCartpage(testData.getTestData("3", "productName")));
			extentTestThread.get().log(Status.PASS, "Product in Cart validated successfully");
		} catch (Throwable t) {
			extentTestThread.get().log(Status.FAIL, "Error in validating the product " + t.getMessage());
			throw t;
		}
		
		try {

			explainedStep("Log out", "Session is closed", () -> homePage.logOutFromApplication());
			extentTestThread.get().log(Status.PASS, "Logged Out from Application successfully");
		} catch (Throwable t) {
			extentTestThread.get().log(Status.FAIL, "Error in logging out from application " + t.getMessage());
			throw t;
		}

	}

	

}
