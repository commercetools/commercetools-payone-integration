package util;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.openqa.selenium.support.ui.ExpectedConditions.not;
import static util.constant.WebDriver3ds.URL_3DS_PATTERN;
import static util.constant.WebDriverCommon.CSS_SELECTOR_PASSWORD;
import static util.constant.WebDriverCommon.CSS_SELECTOR_SEND;

/**
 * @author fhaertig
 * @author Jan Wolter
 * @since 21.01.16
 */
public class WebDriver3ds extends CustomWebDriver {

     public WebDriver3ds() {
        super();
    }

    /**
     * Submits the given {@code password} at the given {@code url}'s "password" element, waits for a redirect and
     * returns the URL it was redirected to.
     *
     * @param url      the URL to navigate to
     * @param password the password
     * @return the URL the browser was redirected to after submitting the {@code password}
     */
    public String execute3dsRedirectWithPassword(final String url, final String password) {
        getDriver().get(url);;

        final WebElement pwInput = findElement(By.cssSelector(CSS_SELECTOR_PASSWORD));
        pwInput.sendKeys(password);
        final WebElement submitButton = findElement(By.cssSelector(CSS_SELECTOR_SEND));
        submitButton.click();

        // Wait for redirect to complete
        WebDriverWait wait = new WebDriverWait(getDriver(), 10);
        wait.until(not(ExpectedConditions.urlContains(URL_3DS_PATTERN)));
        Boolean waitToSuccess = wait.until(ExpectedConditions.urlContains("-Success"));
        return waitToSuccess ? getUrl() : "";
    }

    public void quit() {
        super.quit();
    }
}
