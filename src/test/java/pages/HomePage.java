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
	private SelfHealingLocatorAgent.HealingDecision pendingCartDecision;
    private String cartRepairSource;
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
		pendingCartDecision = null;
		if (Boolean.getBoolean("framework.healing.cart.enabled")) {
			cartRepairSource = StackWalker.getInstance().walk(frames -> frames
                    .filter(f -> f.getClassName().equals(HomePage.class.getName()))
                    .map(f -> "src/test/java/pages/" + f.getFileName() + ":" + f.getLineNumber())
                    .findFirst().orElse("src/test/java/pages/HomePage.java (line unavailable)"));
            SelfHealingLocatorAgent.ResolvedElement resolved;
            try {
                resolved = cartRecovery.findWithApprovedRecovery(
					driver, "Cart link", CART_ORIGINAL,
					List.of(new SelfHealingLocatorAgent.Candidate(CART_APPROVED, 0.90,
							List.of("reviewed-data-test", "cart-link-role"))),
                        element -> support.explainable.CartRecoveryPolicy.accepts(element, driver.getCurrentUrl()));
            } catch (org.openqa.selenium.NoSuchElementException rejected) {
                ExplainableAiIntegration.repairEvent("Attempt 1 failed; automatic repair blocked",
                        rejected.getMessage(), cartRepairSource);
                throw rejected;
            }
			pendingCartDecision = resolved.decision();
			if (resolved.decision() != null) {
                ExplainableAiIntegration.repairEvent("Attempt 1: original locator failed",
                        resolved.decision().failureReason(), cartRepairSource);
                ExplainableAiIntegration.repairEvent("Automatic runtime repair selected",
                        resolved.decision().failedLocator().display() + " -> " + resolved.decision().replacementLocator().display()
                        + ". Replacement passed uniqueness, availability and cart-control safety checks. "
                        + "No Java source line was rewritten. The next click is attempt 2 at this step.", cartRepairSource);
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
	
    /** Call once, immediately after checking the cart navigation postcondition. */
    public void recordCartOutcome(boolean succeeded) {
        if (pendingCartDecision == null) return;
        var verified = cartRecovery.verifyOutcome(pendingCartDecision, succeeded);
        pendingCartDecision = null;
        ExplainableAiIntegration.repairEvent(succeeded ? "Attempt 2: passed after healing" : "Attempt 2: failed after healing",
                verified.businessUpdate() + " This describes cart navigation, not a second complete test run.", cartRepairSource);
        ExplainableAiIntegration.record("Verify cart recovery", "Expected cart page opened",
                verified.businessUpdate() + " " + verified.successRateLabel(),
                succeeded ? ExplainableAiAgent.Outcome.PASS : ExplainableAiAgent.Outcome.FAIL, 0);
    }

	public void logOutFromApplication() {
		hamburgerMenuButton.click();
		logOutButton.click();
	}
	
	

}
