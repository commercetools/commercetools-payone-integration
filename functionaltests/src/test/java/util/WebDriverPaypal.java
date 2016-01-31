package util;

import static org.openqa.selenium.support.ui.ExpectedConditions.not;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author fhaertig
 * @since 22.01.16
 */
public class WebDriverPaypal extends HtmlUnitDriver {

    private static final Logger LOG = LoggerFactory.getLogger(WebDriverPaypal.class);

    private static final int DEFAULT_TIMEOUT = 30;

    public WebDriverPaypal() {
        super(BrowserVersion.FIREFOX_38, false);

        this.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        this.manage().timeouts().pageLoadTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        this.manage().timeouts().setScriptTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);

        getWebClient().setCssErrorHandler(new SilentCssErrorHandler());
        getWebClient().getOptions().setThrowExceptionOnScriptError(false);

        getWebClient().getOptions().setUseInsecureSSL(true);
        getWebClient().getOptions().setRedirectEnabled(true);
        getWebClient().setIncorrectnessListener(new IncorrectnessListener() {
            @Override
            public void notify(final String message, final Object origin) {
                //silent
            }
        });
    }

    private void doLogin(final LoginData loginData) {
        WebElement loginEmailInput = this.findElement(By.id("login_email"));
        WebElement loginPwInput = this.findElement(By.id("login_password"));
        WebElement submitButton = this.findElement(By.id("submitLogin"));

        loginEmailInput.clear();
        loginEmailInput.sendKeys(loginData.getAccountIdentifier());
        loginPwInput.sendKeys(loginData.getPassword());
        submitButton.click();
    }

    /**
     * Executes an order of commands to click through a page flow e.g. for a redirect process.
     *
     * @param url the url where to enter the login information.
     * @param loginData a container providing the account identifier (e.g. email) and password for login.
     * @return the final url the user was redirected to after confirming the payment.
     */
    public String doLoginAndConfirmation(
            final String url,
            final LoginData loginData) {

        final Wait<WebDriver> wait = new WebDriverWait(this, DEFAULT_TIMEOUT);

        this.navigate().to(url);
        doLogin(loginData);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("continue")));

        WebElement payButton = this.findElement(By.id("continue"));
        payButton.click();

        wait.until(not(ExpectedConditions.urlContains("paypal")));

        return this.getCurrentUrl();
    }
}
