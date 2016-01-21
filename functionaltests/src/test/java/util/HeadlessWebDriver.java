package util;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.DefaultCssErrorHandler;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.w3c.css.sac.CSSParseException;

import java.util.concurrent.TimeUnit;

/**
 * @author fhaertig
 * @since 21.01.16
 */
public class HeadlessWebDriver extends HtmlUnitDriver {

    private static final int DEFAULT_TIMEOUT = 5;

    public HeadlessWebDriver() {
        super(BrowserVersion.FIREFOX_38, true);

        this.manage().timeouts().implicitlyWait(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        this.manage().timeouts().pageLoadTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        this.manage().timeouts().setScriptTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);

        getWebClient().waitForBackgroundJavaScript(1000);
        getWebClient().getOptions().setThrowExceptionOnScriptError(true);
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
}
