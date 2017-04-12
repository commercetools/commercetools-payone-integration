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
import static util.constant.WebDriverCommon.XPATH_SUBMIT;
import static util.constant.WebDriverSofortueberweisungConstants.*;

/**
 * @author fhaertig
 * @since 14.03.16
 */
public class WebDriverSofortueberweisung extends HtmlUnitDriver {

    private static final int DEFAULT_TIMEOUT = 5;

    private final String pin;

    private final String tan;

    public WebDriverSofortueberweisung(final String pin, final String tan) {
        super(BrowserVersion.FIREFOX_38, true);

        this.pin = pin;
        this.tan = tan;

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
    }

    private void doLogin(final String userid) {
        final WebElement useridInput = findElement(By.id(SU_LOGIN_NAME_ID));
        final WebElement pinInput = findElement(By.id(SU_USER_PIN_ID));
        useridInput.clear();
        useridInput.sendKeys(userid);
        pinInput.sendKeys(pin);

        final WebElement submitButton = findSubmitButton();
        submitButton.click();
    }

    /**
     * Search for the single submit button on the page by xpath pattern from {@link util.constant.WebDriverCommon#XPATH_SUBMIT}
     * @return found button.
     * @throws {@link RuntimeException} if submit button can't be found or multiply buttons exist on the page
     */
    private WebElement findSubmitButton() {
        // note: earlier we used By.cssSelector("button[type=submit]"), but this does not separate properly other buttons,
        // thus we use now xpath, which looks more proper solutions in this case
        List<WebElement> elements = findElements(By.xpath(XPATH_SUBMIT));
        if (elements == null || elements.size() != 1) {
            throw new RuntimeException("Submit button not found on the page");
        }

        return elements.get(0);
    }


    private void selectAccount() {
        final WebElement senderAccountInput = findElement(By.id(SU_TEST_ACCOUNT_RADIO_BUTTON));
        final WebElement submitButton = findSubmitButton();

        senderAccountInput.click();
        submitButton.click();
    }

    private void provideTan() {
        final WebElement tanInput = findElement(By.id(SU_BACKEND_FORM_TAN));
        final WebElement submitButton = findSubmitButton();

        tanInput.sendKeys(tan);
        submitButton.click();
    }

    /**
     * Submits the given {@code password} at the given {@code url}'s "password" element, waits for a redirect and
     * returns the URL it was redirected to.
     *
     * @param url the URL to navigate to
     * @param userid the account id to use
     * @return the URL the browser was redirected to after submitting the {@code password}
     */
    public String executeSofortueberweisungRedirect(final String url, final String userid) {
        final Wait<WebDriver> wait = new WebDriverWait(this, 20);

        navigate().to(url);

        doLogin(userid);

        wait.until(ExpectedConditions.urlContains(SU_URL_SELECT_ACCOUNT_PATTERN));

        selectAccount();

        wait.until(ExpectedConditions.urlContains(SU_URL_PROVIDE_TAN_PATTERN));

        provideTan();

        wait.until(not(ExpectedConditions.urlContains(SU_URL_PAY_1_PATTERN)));

        return getCurrentUrl();
    }

    public void quit() {
        super.quit();
    }
}
