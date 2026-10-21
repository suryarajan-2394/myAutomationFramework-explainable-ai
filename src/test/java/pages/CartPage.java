package pages;

import java.net.URI;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

public class CartPage extends BasePageClass {
    public CartPage(WebDriver driver) { super(driver); }

    public void assertCartPage(String expectedOriginPage) {
        URI expected = URI.create(expectedOriginPage).resolve("cart.html");
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> {
            URI actual = URI.create(d.getCurrentUrl());
            return java.util.Objects.equals(actual.getScheme(), expected.getScheme())
                    && java.util.Objects.equals(actual.getAuthority(), expected.getAuthority())
                    && java.util.Objects.equals(actual.getPath(), expected.getPath())
                    && actual.getQuery() == null && actual.getFragment() == null
                    && d.findElements(By.cssSelector(".title")).stream().anyMatch(e -> e.isDisplayed() && e.getText().equals("Your Cart"));
        });
    }

    /** A single configured product, with the expected quantity and no unexpected cart rows. */
    public void validateCartpage(String productName) { validateCartContents(productName, 1); }

    public void validateCartContents(String productName, int expectedQuantity) {
        var rows = driver.findElements(By.cssSelector(".cart_item"));
        Assert.assertEquals(rows.size(), 1, "Expected exactly one product row; extra/missing cart items must fail");
        var row = rows.get(0);
        Assert.assertEquals(row.findElement(By.cssSelector(".inventory_item_name")).getText(), productName,
                "Wrong product in cart");
        Assert.assertEquals(row.findElement(By.cssSelector(".cart_quantity")).getText(),
                Integer.toString(expectedQuantity), "Wrong product quantity");
    }
}
