package util;

import org.openqa.selenium.WebElement;

public class GetTextOverride {
	private final WebElement element;

    public GetTextOverride(WebElement element) {
        this.element = element;
    }

    // Custom getText behavior
    public String getText() {
        String raw = element.getText();
        return raw != null ? raw.trim().replaceAll("\\s+", " ") : "";
    }

}
