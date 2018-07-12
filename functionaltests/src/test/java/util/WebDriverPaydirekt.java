package util;


import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * @author fhaertig
 * @since 14.03.16
 */
public class WebDriverPaydirekt extends CustomWebDriver {

    private static String LOGIN_FORM_USERNAME_FIELD_ID = "username";
    private static String LOGIN_FORM_PASSWORD_FIELD_ID = "password";
    private static String LOGIN_FORM_SUBMIT_BUTTON_NAME = "loginBtn";
    private static String PAYMENT_SUBMIT_BUTTON_XPATH = "//BUTTON[@type='submit'][text()=' Jetzt bezahlen ']";


    public WebDriverPaydirekt() {
        super();
    }

    private void doLogin(String userid, String pin) {
        final WebElement useridInput = findElement(By.id(LOGIN_FORM_USERNAME_FIELD_ID));

        useridInput.clear();
        useridInput.sendKeys(userid.trim());

        final WebElement pinInput = findElement(By.id(LOGIN_FORM_PASSWORD_FIELD_ID));
        pinInput.sendKeys(pin.trim());

        final WebElement submitButton = findSubmitButtonByName(LOGIN_FORM_SUBMIT_BUTTON_NAME);
        submitButton.click();
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
        getDriver().get(url);
        doLogin(userid, pin);
        WebDriverWait wait = new WebDriverWait(getDriver(), 10);
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath(PAYMENT_SUBMIT_BUTTON_XPATH))).click();
        Boolean waitToSuccess = wait.until(ExpectedConditions.urlContains("-Success"));
        return waitToSuccess ? getUrl() : "";
    }

    public void quit() {
        super.quit();
    }
}
