package support.explainable;

import java.nio.file.Path;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.HomePage;

/** Run explicitly with -Dtest=CartLocatorFixtureTest after a local ChromeDriver is available. */
public class CartLocatorFixtureTest {
    private WebDriver driver;
    private String previousSetting;

    @BeforeMethod
    public void setup() {
        previousSetting = System.getProperty("framework.healing.cart.enabled");
        System.setProperty("framework.healing.cart.enabled", "true");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        driver = new ChromeDriver(options);
        ExplainableAiIntegration.start("CartLocatorFixtureTest");
    }

    @AfterMethod(alwaysRun = true)
    public void teardown() {
        if (driver != null) driver.quit();
        if (previousSetting == null) System.clearProperty("framework.healing.cart.enabled");
        else System.setProperty("framework.healing.cart.enabled", previousSetting);
    }

    private void open(String file) {
        driver.get(Path.of("src", "test", "resources", "locator-fixture", file)
                .toAbsolutePath().toUri().toString());
    }

    @Test
    public void originalLocatorNeedsNoHeal() {
        open("original.html");
        new HomePage(driver).clickOnCart();
        Assert.assertTrue(driver.getCurrentUrl().endsWith("/cart.html"));
    }

    @Test
    public void approvedUniqueCandidateRecoversChangedClass() {
        open("changed-class.html");
        new HomePage(driver).clickOnCart();
        Assert.assertTrue(driver.getCurrentUrl().endsWith("/cart.html"));
    }

    @Test
    public void plainSeleniumFailsOnChangedClass() {
        System.setProperty("framework.healing.cart.enabled", "false");
        open("changed-class.html");
        Assert.expectThrows(NoSuchElementException.class, () -> new HomePage(driver).clickOnCart());
        Assert.assertTrue(driver.getCurrentUrl().endsWith("/changed-class.html"));
    }

    @Test
    public void ambiguousCandidatesAreRejectedBeforeClick() {
        open("duplicate.html");
        Assert.expectThrows(NoSuchElementException.class, () -> new HomePage(driver).clickOnCart());
        Assert.assertTrue(driver.getCurrentUrl().endsWith("/duplicate.html"), "A wrong target was clicked");
    }
}
