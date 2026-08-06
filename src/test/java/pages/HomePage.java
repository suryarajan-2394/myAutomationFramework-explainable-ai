package pages;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class HomePage extends BasePageClass{

	public HomePage(WebDriver driver) {
		super(driver);
	}
	
	@FindBy(xpath = "//a[@class='shopping_cart_link']")
	WebElement cartIcon;
	
	@FindBy(id = "react-burger-menu-btn")
	WebElement hamburgerMenuButton;
	
	@FindBy(xpath = "//a[text()='Logout']")
	WebElement logOutButton;
	
	public void selectProduct(String productName) {
		String removeFromCartXpath = "//div[text()='%s']/following::button[text()='Remove']";
		List<WebElement> removeButton = driver.findElements(By.xpath("//button[text()='Remove']"));
		if(removeButton.size() != 0) {
			WebElement removeFromCart = dynamicXpath(removeFromCartXpath, productName);
			removeFromCart.click();
		}
		String productSelectionXpath = "//div[text()='%s']/following::button[text()='Add to cart']";
		WebElement productSelection= dynamicXpath(productSelectionXpath, productName);
		productSelection.click();
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
	}
	
	public void clickOnCart() {
		cartIcon.click();
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
	}
	
	public void logOutFromApplication() {
		hamburgerMenuButton.click();
		logOutButton.click();
	}
	
	

}
