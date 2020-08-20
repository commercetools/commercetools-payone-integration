package util;


import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


public class CustomWebDriver {
    private ChromeDriver driver;

    CustomWebDriver() {
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver(getChromeOptions());
        //driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
    }

    private static ChromeOptions getChromeOptions() {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless");
        return chromeOptions;
    }

    protected WebElement findElement(By condition) {
        WebDriverWait wait = new WebDriverWait(driver, 10);
        return wait.until(ExpectedConditions.elementToBeClickable(condition));
    }

    public ChromeDriver getDriver() {
        return driver;
    }

    public void quit() {
        driver.quit();
    }

    public void deleteCookies() {
        driver.manage().deleteAllCookies();
    }

    public String getUrl() {
        return driver.getCurrentUrl();
    }

}
