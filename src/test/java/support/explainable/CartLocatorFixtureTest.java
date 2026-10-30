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
            String browserBinary = System.getProperty("framework.browser.binary");
            if (browserBinary != null && !browserBinary.isBlank()) options.setBinary(browserBinary);
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
    @Test
    public void javascriptCartControlRecoversAndReportsBothAttempts() throws Exception {
        open("inventory.html");
        String inventoryUrl = driver.getCurrentUrl();
        HomePage page = new HomePage(driver);
        page.clickOnCart();
        Assert.assertTrue(driver.getCurrentUrl().endsWith("/cart.html"));
        pages.CartPage cart = new pages.CartPage(driver);
        cart.assertCartPage(inventoryUrl);
        cart.validateCartContents("Fixture product", 1);
        page.recordCartOutcome(true);
        Path directory = java.nio.file.Files.createTempDirectory("repair-evidence-");
        Path html = directory.resolve("explainable/explanation-fixture.html");
        try {
            ExplainableAiIntegration.finish(true, false, null, directory.resolve("explainable"), "fixture");
            String content = java.nio.file.Files.readString(html);
            Assert.assertTrue(content.contains("Attempt 1: original locator failed"));
            Assert.assertTrue(content.contains("Automatic runtime repair selected"));
            Assert.assertTrue(content.contains("Attempt 2: passed after healing"));
            Assert.assertTrue(content.contains("HomePage.java:"));
            Assert.assertTrue(content.contains("No Java source line was rewritten"));
        } finally {
            java.nio.file.Files.deleteIfExists(html);
            java.nio.file.Files.deleteIfExists(directory.resolve("explainable"));
            java.nio.file.Files.deleteIfExists(directory);
        }
    }
    @Test public void productSelectionDoesNotTouchAnotherSelectedProduct() {
        open("products.html");
        HomePage home = new HomePage(driver);
        home.selectProduct("Wanted");
        home.selectProduct("Wanted");
        Assert.assertEquals(driver.findElement(org.openqa.selenium.By.id("wanted")).getAttribute("data-count"), "1");
        Assert.assertNull(driver.findElement(org.openqa.selenium.By.id("other")).getAttribute("data-clicked"));
    }
    @Test public void missingProductActionNeverClicksTheNextProduct() {
        open("product-missing-button.html");
        Assert.expectThrows(NoSuchElementException.class, () -> new HomePage(driver).selectProduct("Wanted"));
        Assert.assertNull(driver.findElement(org.openqa.selenium.By.id("other")).getAttribute("data-clicked"));
    }
}
