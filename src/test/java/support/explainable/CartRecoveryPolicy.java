package support.explainable;

import java.net.URI;
import org.openqa.selenium.WebElement;

/** Cart-specific constraint; business assertions must still check the result after clicking. */
public final class CartRecoveryPolicy {
    private CartRecoveryPolicy() { }
    public static boolean accepts(WebElement element, String pageUrl) {
        if (!"a".equalsIgnoreCase(element.getTagName())
                || "true".equalsIgnoreCase(element.getAttribute("aria-disabled"))) return false;
        String href = element.getAttribute("href");
        String role = element.getAttribute("role");
        if (href != null && !href.isBlank()) {
            if (role != null && !role.isBlank() && !"link".equalsIgnoreCase(role)) return false;
            return destinationAllowed(pageUrl, href);
        }
        // Reviewed SauceDemo SPA control: no href, role=button, stable test identity and accessible name.
        return "button".equalsIgnoreCase(role)
                && "shopping-cart-link".equals(element.getAttribute("data-test"))
                && element.getAttribute("aria-label") != null
                && element.getAttribute("aria-label").startsWith("Cart,")
                && inventoryPage(pageUrl);
    }
    private static boolean inventoryPage(String url) {
        try { return URI.create(url).getPath().endsWith("/inventory.html"); }
        catch (IllegalArgumentException invalid) { return false; }
    }
    public static boolean destinationAllowed(String pageUrl, String href) {
        if (href == null || href.isBlank()) return false;
        try {
            URI page = URI.create(pageUrl);
            URI expected = page.resolve("cart.html").normalize();
            URI actual = page.resolve(href).normalize();
            return java.util.Objects.equals(expected.getScheme(), actual.getScheme())
                    && java.util.Objects.equals(expected.getAuthority(), actual.getAuthority())
                    && java.util.Objects.equals(expected.getPath(), actual.getPath())
                    && actual.getQuery() == null && actual.getFragment() == null;
        } catch (IllegalArgumentException invalid) { return false; }
    }
}
