package pages;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Product identity and actions stay within one inventory card; selection is idempotent. */
final class ProductSelection {
    private ProductSelection() { }
    static void select(WebDriver driver, String productName) {
        selectFromCards(driver.findElements(By.cssSelector(".inventory_item")), productName);
    }
    static void selectFromCards(List<WebElement> cards, String productName) {
        var matches = cards.stream().filter(card -> card.findElements(By.cssSelector(".inventory_item_name"))
                .stream().anyMatch(name -> productName.equals(name.getText()))).toList();
        if (matches.size() != 1) throw new NoSuchElementException("Expected one inventory card for " + productName + "; found " + matches.size());
        var buttons = matches.get(0).findElements(By.tagName("button"));
        if (buttons.size() != 1) throw new NoSuchElementException("Expected one action button inside product card " + productName);
        WebElement button = buttons.get(0);
        if (!button.isDisplayed() || !button.isEnabled()) throw new NoSuchElementException("Product action unavailable for " + productName);
        if ("Remove".equals(button.getText())) return;
        if (!"Add to cart".equals(button.getText())) throw new NoSuchElementException("Unexpected product action for " + productName);
        button.click();
    }
}
