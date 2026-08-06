package pages;

import java.time.Duration;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.testng.Assert;

import util.GetTextOverride;

public class LoginPage extends BasePageClass{

	public LoginPage(WebDriver driver) {
		super(driver);
	}
	
	@FindBy(id = "user-name")
	WebElement userNameField;
	
	@FindBy(id = "password")
	WebElement passwordField;
	
	@FindBy(id = "login-button")
	WebElement loginButton;
	
	@FindBy(xpath = "//h3[@data-test='error']")
	WebElement errorMessage;
	
	public void loginFunction(String userName,String password) {
		userNameField.sendKeys(userName);
		passwordField.sendKeys(password);
		loginButton.click();
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
	}
	
	public void verifyErrorLoginMessage(String message) {
		String errorMessageFromApplication = errorMessage.getText();
		Assert.assertTrue(errorMessageFromApplication.contains(message));
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
	}
	
	public void verifyErrorMessageUsingCustomElement(String message) {
		GetTextOverride customError = new GetTextOverride(errorMessage);
		String processedError = customError.getText();
		System.out.println(processedError);
		Assert.assertTrue(processedError.contains(message));
	}

}
