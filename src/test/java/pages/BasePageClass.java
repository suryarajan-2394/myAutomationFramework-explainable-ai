package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import util.TestData;

public class BasePageClass {
    protected WebDriver driver;
    protected TestData data;
   
    

    public BasePageClass(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
       
    }
    
    public WebElement dynamicXpath(String xpathPattern, String value) {
        return driver.findElement(By.xpath(String.format(xpathPattern, value)));
    }

}
