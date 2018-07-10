package util;


import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.concurrent.TimeUnit;

/**
 * @author fhaertig
 * @since 14.03.16
 */
public class CustomWebDriver {

    private static ChromeOptions getChromeOptions() {
        ChromeOptions chromeOptions = new ChromeOptions();
        return chromeOptions;
    }

    protected ChromeDriver getChromeDriver() {
        System.setProperty("webdriver.chrome.driver", "/webdriver/chromedrive_ios_2.4");
        ChromeDriver driver = new ChromeDriver(getChromeOptions());
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        return driver;

    }
}
