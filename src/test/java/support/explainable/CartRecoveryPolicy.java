package support.explainable;

import java.net.URI;
import org.openqa.selenium.WebElement;

/** Cart-specific constraint; business assertions must still check the result after clicking. */
public final class CartRecoveryPolicy {
    private CartRecoveryPolicy() { }
    public static boolean accepts(WebElement element, String pageUrl) {
        if (!"a".equalsIgnoreCase(element.getTagName())
                || "true".equalsIgnoreCase(element.getAttribute("aria-disabled"))) return false;
        String role = element.getAttribute("role");
        if (role != null && !role.isBlank() && !"link".equalsIgnoreCase(role)) return false;
        return destinationAllowed(pageUrl, element.getAttribute("href"));
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
