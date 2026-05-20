package seleniumTestNGProject;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SampleTest {
    @Test
    public void testSum() {
        int a = 5, b = 10;
        int sum = a + b;
        Assert.assertEquals(sum, 15, "Sum calculation failed!");
    }
}
