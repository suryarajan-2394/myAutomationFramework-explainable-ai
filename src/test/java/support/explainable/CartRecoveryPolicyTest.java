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
}
