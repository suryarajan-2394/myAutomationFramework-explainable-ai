package testCase;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

import com.aventstack.extentreports.Status;

public class CartTest extends BaseTest {

	@Test
	public void AddToCartTestCase() throws Throwable {

		driver.get(URL);
		driver.manage().window().maximize();
		driver.navigate().refresh();

		try {

			explainedStep("Log in", "Inventory page is displayed", () -> loginPage.loginFunction(testData.getTestData("3", "userName"), testData.getTestData("3", "password")),
					() -> Assert.assertTrue(new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> d.getCurrentUrl().contains("/inventory.html")), "Inventory URL was not reached"));
			extentTestThread.get().log(Status.PASS, "Login action performed successfully");
		} catch (Throwable t) {
			extentTestThread.get().log(Status.FAIL, "Error during login: " + t.getMessage());
			throw t;
		}
		
		try {

			explainedStep("Add configured product", "Cart badge shows one item", () -> homePage.selectProduct(testData.getTestData("3", "productName")),
					() -> Assert.assertEquals(new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> d.findElement(By.cssSelector(".shopping_cart_badge")).getText()), "1"));
			extentTestThread.get().log(Status.PASS, "Product is selected and is added to Cart successfully");
		} catch (Throwable t) {
			extentTestThread.get().log(Status.FAIL, "Error during adding products to Cart: " + t.getMessage());
			throw t;
		}
		
		try {

			explainedStep("Open cart", "Cart page is displayed", () -> homePage.clickOnCart(),
					() -> Assert.assertTrue(new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> d.getCurrentUrl().contains("/cart.html")), "Cart URL was not reached"));
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

			explainedStep("Log out", "Login screen is displayed", () -> homePage.logOutFromApplication(),
					() -> Assert.assertTrue(new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> d.findElement(By.id("login-button")).isDisplayed()), "Login screen was not displayed"));
			extentTestThread.get().log(Status.PASS, "Logged Out from Application successfully");
		} catch (Throwable t) {
			extentTestThread.get().log(Status.FAIL, "Error in logging out from application " + t.getMessage());
			throw t;
		}

	}

	

}
