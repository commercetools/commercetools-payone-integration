package util;


import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;

/**
 * @author fhaertig
 * @since 14.03.16
 */
public class WebDriverPaydirekt extends CustomWebDriver {

    private static String LOGIN_FORM_USERNAME_FIELD_ID = "username";
    private static String LOGIN_FORM_PASSWORD_FIELD_ID = "password";
    private static String LOGIN_FORM_SUBMIT_BUTTON_NAME = "loginBtn";
    private static String CONFIRM_FORM_NAME = "firstFactorAuthForm";
    private static String CONFIRM_BUTTON_NAME = "confirmPaymentButton";
    private static String URL_SUCCESS_PATTERN = "-Success";
    private static Logger LOG = LoggerFactory.getLogger(WebDriverPaydirekt.class);

    public WebDriverPaydirekt() {
        super();
    }

    private boolean doLogin(String userid, String pin) {
        boolean loggedIn = false;
        final WebElement useridInput = findElement(By.id(LOGIN_FORM_USERNAME_FIELD_ID));

        useridInput.clear();
        useridInput.sendKeys(userid.trim());

        final WebElement pinInput = findElement(By.id(LOGIN_FORM_PASSWORD_FIELD_ID));
        pinInput.sendKeys(pin.trim());

        final WebElement submitButton = findElement(By.name(LOGIN_FORM_SUBMIT_BUTTON_NAME));
        if (submitButton == null) {
            LOG.error(String.format("Submit button with name %S not found on the page",
                    LOGIN_FORM_SUBMIT_BUTTON_NAME));
        } else {
            loggedIn = true;
            submitButton.click();

        }
        return loggedIn;
    }


    /**
     * Submits the given {@code password} at the given {@code url}'s "password" element, waits for a redirect and
     * returns the URL it was redirected to.
     *
     * @param url    the URL to navigate to
     * @param userid the account id to use
     * @return the URL the browser was redirected to after submitting the {@code password}
     */
    public String executePayDirectRedirect(final String url, final String userid, final String pin) {
        boolean success = false;
        getDriver().get(url);
        if (doLogin(userid, pin)) {
            WebDriverWait wait = new WebDriverWait(getDriver(), 10);
            wait.until(elementToBeClickable(findElement(By.name(CONFIRM_BUTTON_NAME)))).click();
            success = wait.until(ExpectedConditions.urlContains(URL_SUCCESS_PATTERN));
        }
        return success ? getUrl() : "";
    }
}
