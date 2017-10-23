package util;


import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.openqa.selenium.support.ui.ExpectedConditions.not;
import static util.constant.WebDriverCommon.CLASS_NAME_PRIMARY;
import static util.constant.WebDriverSofortueberweisungConstants.*;

/**
 * @author fhaertig
 * @since 14.03.16
 */
public class WebDriverSofortueberweisung extends HtmlUnitDriver {

    private static final int DEFAULT_TIMEOUT = 5;

    private final Wait<WebDriver> wait;

    public WebDriverSofortueberweisung() {
        super(BrowserVersion.FIREFOX_45, true);

        final Timeouts timeouts = manage().timeouts();
        timeouts.implicitlyWait(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        timeouts.pageLoadTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        timeouts.setScriptTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);

        manage().window().fullscreen();

        final WebClient webClient = getWebClient();
        webClient.setJavaScriptTimeout(TimeUnit.SECONDS.toMillis(DEFAULT_TIMEOUT));
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setPopupBlockerEnabled(true);

        webClient.setIncorrectnessListener((message, origin) -> {
            //swallow these messages
        });
        webClient.setCssErrorHandler(new SilentCssErrorHandler());

        this.wait = new WebDriverWait(this, 20);
    }

    private void doLogin(String userid, String pin) {
        final WebElement useridInput = findElement(By.id(SU_LOGIN_NAME_ID));

        wait.until(ExpectedConditions.elementToBeClickable(useridInput));
        useridInput.clear();
        useridInput.sendKeys(userid);

        final WebElement pinInput = findElement(By.id(SU_USER_PIN_ID));
        wait.until(ExpectedConditions.elementToBeClickable(pinInput));
        pinInput.sendKeys(pin);

        final WebElement submitButton = findSubmitButton();
        wait.until(ExpectedConditions.elementToBeClickable(submitButton));
        submitButton.click();
    }

    /**
     * Search for the single submit button on the page by className pattern from
     * {@link util.constant.WebDriverCommon#CLASS_NAME_PRIMARY}
     *
     * @return found button.
     * @throws RuntimeException if submit button can't be found or multiply buttons exist on the page
     */
    private WebElement findSubmitButton() {
        List<WebElement> elements = findElements(By.className(CLASS_NAME_PRIMARY));
        if (elements == null || elements.size() != 1) {
            throw new RuntimeException("Submit button not found on the page");
        }

        return elements.get(0);
    }


    private void selectAccount() {
        final WebElement senderAccountInput = findElement(By.id(SU_TEST_ACCOUNT_RADIO_BUTTON));

        wait.until(ExpectedConditions.elementToBeClickable(senderAccountInput));
        senderAccountInput.click();

        final WebElement submitButton = findSubmitButton();
        wait.until(ExpectedConditions.elementToBeClickable(submitButton));
        submitButton.click();
    }

    private void provideTan(String tan) {
        final WebElement tanInput = findElement(By.id(SU_BACKEND_FORM_TAN));
        final WebElement submitButton = findSubmitButton();

        wait.until(ExpectedConditions.elementToBeClickable(tanInput));
        tanInput.sendKeys(tan);

        wait.until(ExpectedConditions.elementToBeClickable(submitButton));
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
    public String executeSofortueberweisungRedirect(final String url, final String userid, final String pin, final String tan) {
        navigate().to(url);
        doLogin(userid, pin);

        wait.until(ExpectedConditions.urlContains(SU_URL_SELECT_ACCOUNT_PATTERN));
        selectAccount();

        wait.until(ExpectedConditions.urlContains(SU_URL_PROVIDE_TAN_PATTERN));
        provideTan(tan);

        wait.until(not(ExpectedConditions.urlContains(SU_URL_PAY_1_PATTERN)));

        return getCurrentUrl();
    }

    public void quit() {
        super.quit();
    }
}
