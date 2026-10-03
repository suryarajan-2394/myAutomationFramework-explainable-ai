package pages;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import support.explainable.ExplainableAiAgent;
import support.explainable.ExplainableAiIntegration;
import support.explainable.SelfHealingLocatorAgent;

public class HomePage extends BasePageClass{
	private static final SelfHealingLocatorAgent.Locator CART_ORIGINAL =
			new SelfHealingLocatorAgent.Locator(SelfHealingLocatorAgent.Strategy.CSS, "a.shopping_cart_link");
	private static final SelfHealingLocatorAgent.Locator CART_APPROVED =
			new SelfHealingLocatorAgent.Locator(SelfHealingLocatorAgent.Strategy.CSS, "a[data-test='shopping-cart-link']");
	private final SelfHealingLocatorAgent cartRecovery = new SelfHealingLocatorAgent();

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
		if (Boolean.getBoolean("framework.healing.cart.enabled")) {
			SelfHealingLocatorAgent.ResolvedElement resolved = cartRecovery.findWithApprovedRecovery(
					driver, "Cart link", CART_ORIGINAL,
					List.of(new SelfHealingLocatorAgent.Candidate(CART_APPROVED, 0.90,
							List.of("reviewed-data-test", "cart-link-role"))));
			if (resolved.decision() != null) {
				ExplainableAiIntegration.record("Cart locator recovery", "Unique, visible and enabled cart link",
						"Candidate selected (navigation still requires verification): "
								+ resolved.decision().failedLocator().display() + " -> "
								+ resolved.decision().replacementLocator().display(),
						ExplainableAiAgent.Outcome.PASS, 0);
			}
			resolved.element().click();
		} else {
			cartIcon.click();
		}
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
	}
	
	public void logOutFromApplication() {
		hamburgerMenuButton.click();
		logOutButton.click();
	}
	
	

}
