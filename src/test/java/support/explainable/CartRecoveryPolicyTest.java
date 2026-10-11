package support.explainable;
import org.testng.Assert;
import org.testng.annotations.Test;
public class CartRecoveryPolicyTest {
    @Test public void requiresTheExpectedCartDestination() {
        Assert.assertTrue(CartRecoveryPolicy.destinationAllowed("https://shop.example/inventory.html", "cart.html"));
        Assert.assertTrue(CartRecoveryPolicy.destinationAllowed("file:///fixtures/changed-class.html", "file:///fixtures/cart.html"));
        Assert.assertFalse(CartRecoveryPolicy.destinationAllowed("https://shop.example/inventory.html", "wrong.html"));
        Assert.assertFalse(CartRecoveryPolicy.destinationAllowed("https://shop.example/inventory.html", "https://other.example/cart.html"));
        Assert.assertFalse(CartRecoveryPolicy.destinationAllowed("https://shop.example/inventory.html", "javascript:alert(1)"));
        Assert.assertFalse(CartRecoveryPolicy.destinationAllowed("https://shop.example/inventory.html", null));
    }
    @Test public void acceptsReviewedJavascriptCartButNotUnidentifiedControl() {
        Assert.assertTrue(CartRecoveryPolicy.accepts(control("button", "shopping-cart-link", "Cart, 1 items"),
                "https://www.saucedemo.com/inventory.html"));
        Assert.assertFalse(CartRecoveryPolicy.accepts(control("button", "other", "Cart, 1 items"),
                "https://www.saucedemo.com/inventory.html"));
        Assert.assertFalse(CartRecoveryPolicy.accepts(control("button", "shopping-cart-link", "Checkout"),
                "https://www.saucedemo.com/inventory.html"));
    }
    private static org.openqa.selenium.WebElement control(String role, String identity, String label) {
        return (org.openqa.selenium.WebElement) java.lang.reflect.Proxy.newProxyInstance(
                getClassLoader(), new Class<?>[]{org.openqa.selenium.WebElement.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getTagName")) return "a";
                    if (method.getName().equals("getAttribute")) return switch ((String) args[0]) {
                        case "role" -> role;
                        case "data-test" -> identity;
                        case "aria-label" -> label;
                        default -> null;
                    };
                    throw new UnsupportedOperationException(method.getName());
                });
    }
    private static ClassLoader getClassLoader() { return CartRecoveryPolicyTest.class.getClassLoader(); }
}
