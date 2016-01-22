package util;

import static org.openqa.selenium.support.ui.ExpectedConditions.not;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.DefaultCssErrorHandler;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.w3c.css.sac.CSSParseException;

import java.util.concurrent.TimeUnit;

/**
 * @author fhaertig
 * @since 21.01.16
 */
public class WebDriver3ds extends HtmlUnitDriver {

    private static final int DEFAULT_TIMEOUT = 5;

    public WebDriver3ds() {
        super(BrowserVersion.FIREFOX_38, true);

        this.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        this.manage().timeouts().pageLoadTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        this.manage().timeouts().setScriptTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);

        getWebClient().setJavaScriptTimeout(2000);
        getWebClient().getOptions().setThrowExceptionOnScriptError(false);
        getWebClient().getOptions().setPopupBlockerEnabled(true);
        getWebClient().setCssErrorHandler(new DefaultCssErrorHandler() {

            @Override
            public void error(final CSSParseException exception) {
                //leave empty for silencing warnings about css error etc.
            }

            @Override
            public void warning(final CSSParseException exception) {
                //leave empty for silencing warnings about css error etc.
            }
        });
    }

    public String execute3dsRedirectWithPassword(final String url, final String password) throws InterruptedException {
        this.navigate().to(url);
        WebElement element = this.findElement(By.xpath("//input[@name=\"password\"]"));
        element.sendKeys(password);
        element.submit();

        // Wait for redirect to complete
        Wait<WebDriver> wait = new WebDriverWait(this, 10);
        wait.until(not(ExpectedConditions.urlContains("3ds")));
        return this.getCurrentUrl();
    }

    public void quit() {
        super.quit();
    }
}
