package pages;

import java.lang.reflect.Proxy;
import java.util.List;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CartPageEvidenceTest {
    private WebElement text(String value) {
        return (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{WebElement.class},
                (proxy, method, args) -> { if (method.getName().equals("getText")) return value; throw new UnsupportedOperationException(method.getName()); });
    }
    private WebElement row(String product, String quantity) {
        return (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{WebElement.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findElement"))
                        return text(args[0].toString().contains("inventory_item_name") ? product : quantity);
                    throw new UnsupportedOperationException(method.getName());
                });
    }
    private CartPage page(List<WebElement> rows) {
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{WebDriver.class},
                (proxy, method, args) -> { if (method.getName().equals("findElements")) return rows; throw new UnsupportedOperationException(method.getName()); });
        return new CartPage(driver);
    }
    @Test public void acceptsExactlyTheRequiredProductAndQuantity() { page(List.of(row("Backpack", "1"))).validateCartContents("Backpack", 1); }
    @Test public void rejectsWrongProduct() { Assert.expectThrows(AssertionError.class, () -> page(List.of(row("Other", "1"))).validateCartContents("Backpack", 1)); }
    @Test public void rejectsWrongQuantity() { Assert.expectThrows(AssertionError.class, () -> page(List.of(row("Backpack", "2"))).validateCartContents("Backpack", 1)); }
    @Test public void rejectsAdditionalItems() { Assert.expectThrows(AssertionError.class, () -> page(List.of(row("Backpack", "1"), row("Other", "1"))).validateCartContents("Backpack", 1)); }
}
