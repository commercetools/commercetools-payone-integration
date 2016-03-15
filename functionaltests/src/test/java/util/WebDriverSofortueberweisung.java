package util;


import static org.openqa.selenium.support.ui.ExpectedConditions.not;

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

import java.util.concurrent.TimeUnit;

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

        final WebClient webClient = getWebClient();
        webClient.setJavaScriptTimeout(5000);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setPopupBlockerEnabled(true);

        webClient.setIncorrectnessListener((message, origin) -> {
            //swallow these messages
        });
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
    }

    private void doLogin(final String userid) {
        final WebElement useridInput = findElement(By.id("BackendFormLOGINNAMEUSERID"));
        final WebElement pinInput = findElement(By.id("BackendFormUSERPIN"));
        final WebElement submitButton = findElement(By.cssSelector("button[type=submit]"));

        useridInput.clear();
        useridInput.sendKeys(userid);
        pinInput.sendKeys(pin);
        submitButton.click();
    }


    private void selectAccount() {
        final WebElement senderAccountInput = findElement(By.id("TransactionsSessionSenderAccountNumber23456789"));
        final WebElement submitButton = findElement(By.cssSelector("button[type=submit]"));

        senderAccountInput.click();
        submitButton.click();
    }

    private void provideTan() {
        final WebElement tanInput = findElement(By.id("BackendFormTan"));
        final WebElement submitButton = findElement(By.cssSelector("button[type=submit]"));

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
        final Wait<WebDriver> wait = new WebDriverWait(this, 10);

        navigate().to(url);

        doLogin(userid);

        wait.until(ExpectedConditions.urlContains("select_account"));

        selectAccount();

        wait.until(ExpectedConditions.urlContains("provide_tan"));

        provideTan();

        wait.until(not(ExpectedConditions.urlContains("pay1")));

        return getCurrentUrl();
    }

    public void quit() {
        super.quit();
    }
}
