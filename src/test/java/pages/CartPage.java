package pages;

import java.time.Duration;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.testng.Assert;

public class CartPage extends BasePageClass{

	public CartPage(WebDriver driver) {
		super(driver);
	}
	
	@FindBy(xpath= "//div[@class='inventory_item_name']")
	WebElement productInCart;
	
	public void validateCartpage(String productName) {
		String productAddedInCart = productInCart.getText();
		Assert.assertTrue(productName.equalsIgnoreCase(productAddedInCart));
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
	}
	
	
	
	

}
