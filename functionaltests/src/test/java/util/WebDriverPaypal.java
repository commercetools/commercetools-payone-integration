package util;

import static org.openqa.selenium.support.ui.ExpectedConditions.not;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
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
public class WebDriverPaypal extends HtmlUnitDriver{

    private static final Logger LOG = LoggerFactory.getLogger(WebDriverPaypal.class);

    private static final int DEFAULT_TIMEOUT = 10;

    public WebDriverPaypal() {
        super(BrowserVersion.FIREFOX_38, false);

        this.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        this.manage().timeouts().pageLoadTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        this.manage().timeouts().setScriptTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);

        final WebClient webClient = getWebClient();
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setPopupBlockerEnabled(true);

        webClient.setIncorrectnessListener((message, origin) -> {
            //swallow these messages
        });
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
    }

    public void doLogin(final String email, final String password) {
        try {
            WebElement loginEmailInput = this.findElement(By.id("login_email"));
            WebElement loginPwInput = this.findElement(By.id("login_password"));
            loginEmailInput.sendKeys(email);
            loginPwInput.sendKeys(password);
            loginPwInput.submit();
        } catch (NoSuchElementException ex) {
            LOG.info("already logged in or login input fields not found.");
        }
    }


    public String executePaypalPayment(final String url, final String email, final String password) {
        this.navigate().to(url);
        doLogin(email, password);

        WebElement payButton = this.findElement(By.xpath("//input[@id=\"continue\"]"));
        payButton.submit();

        final Wait<WebDriver> wait = new WebDriverWait(this, 10);
        wait.until(not(ExpectedConditions.urlContains("paypal")));

        return this.getCurrentUrl();
    }
}
