package util;


import org.apache.commons.lang3.SystemUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


public class CustomWebDriver {
    private ChromeDriver driver;

    protected CustomWebDriver() {
        String currentDriver = SystemUtils.IS_OS_MAC ? "chromedriver_ios_2.4" : "chromedriver_unix_2.4";
        System.setProperty("webdriver.chrome.driver", getClass().getClassLoader().getResource
                ("webdriver/" + currentDriver).getPath());
        driver = new ChromeDriver(getChromeOptions());
        //(driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
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

    /**
     * Search for the single submit button on the page by className pattern from
     * {@link util.constant.WebDriverCommon#CLASS_NAME_PRIMARY}
     *
     * @return found button.
     * @throws RuntimeException if submit button can't be found or multiply buttons exist on the page
     */
    protected WebElement findSubmitButtonByName(String name) {

        WebElement elements = findElement(By.name(name));
        if (elements == null) {
            throw new RuntimeException("Submit button not found on the page");
        }

        return elements;
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
