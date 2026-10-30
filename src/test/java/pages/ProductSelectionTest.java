package pages;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.NoSuchElementException;
import org.testng.Assert;
import org.testng.annotations.Test;
public class ProductSelectionTest {
    private WebElement element(String text, List<WebElement> children, AtomicInteger clicks) {
        return (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{WebElement.class}, (proxy, method, args) -> switch(method.getName()) {
            case "getText" -> text;
            case "isDisplayed", "isEnabled" -> true;
            case "click" -> { clicks.incrementAndGet(); yield null; }
            case "findElements" -> children;
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }
    private WebElement card(String name, List<WebElement> buttons) {
        return (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{WebElement.class}, (proxy, method, args) -> {
            if (method.getName().equals("findElements")) return args[0].toString().contains("inventory_item_name")
                    ? List.of(element(name, List.of(), new AtomicInteger())) : buttons;
            throw new UnsupportedOperationException(method.getName());
        });
    }
    @Test public void otherSelectedProductIsUntouched() {
        var target = new AtomicInteger(); var other = new AtomicInteger();
        ProductSelection.selectFromCards(List.of(card("Other",List.of(element("Remove",List.of(),other))),card("Wanted",List.of(element("Add to cart",List.of(),target)))),"Wanted");
        Assert.assertEquals(target.get(),1); Assert.assertEquals(other.get(),0);
    }
    @Test public void missingTargetButtonDoesNotClickNextProduct() {
        var clicks = new AtomicInteger();
        Assert.expectThrows(NoSuchElementException.class, () -> ProductSelection.selectFromCards(List.of(card("Wanted",List.of()),card("Other",List.of(element("Add to cart",List.of(),clicks)))),"Wanted"));
        Assert.assertEquals(clicks.get(),0);
    }
    @Test public void duplicateNamesAreRejected() {
        var clicks=new AtomicInteger(); var button=element("Add to cart",List.of(),clicks);
        Assert.expectThrows(NoSuchElementException.class, () -> ProductSelection.selectFromCards(List.of(card("Wanted",List.of(button)),card("Wanted",List.of(button))),"Wanted"));
        Assert.assertEquals(clicks.get(),0);
    }
    @Test public void alreadySelectedIsIdempotent() {
        var clicks=new AtomicInteger();
        ProductSelection.selectFromCards(List.of(card("Wanted",List.of(element("Remove",List.of(),clicks)))),"Wanted");
        Assert.assertEquals(clicks.get(),0);
    }
}
