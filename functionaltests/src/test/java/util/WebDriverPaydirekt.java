package util;


import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author fhaertig
 * @since 14.03.16
 */
public class WebDriverPaydirekt extends HtmlUnitDriver {

    public static final String SU_URL_PAY_1_PATTERN = "pay1";
    private static final int DEFAULT_TIMEOUT = 20;
    private static final String LOGIN_FORM_NAME = "loginForm";
    private static String LOGIN_FORM_USERNAME_FIELD_ID = "username";
    private static String LOGIN_FORM_PASSWORD_FIELD_ID = "password";
    private static String LOGIN_FORM_SUBMIT_BUTTON_NAME = "loginBtn";
    private static String PAYMENT_SUBMIT_BUTTON_NAME = "firstFactorAuthForm";
    private final Wait<WebDriver> wait;


    public WebDriverPaydirekt() {
        super(BrowserVersion.CHROME, false);

        final WebDriver.Timeouts timeouts = manage().timeouts();
        timeouts.implicitlyWait(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        timeouts.pageLoadTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        timeouts.setScriptTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);


        this.wait = new WebDriverWait(this, 20);
    }

    private void doLogin(String userid, String pin) {
        wait.until(ExpectedConditions.elementToBeClickable(By.id(LOGIN_FORM_USERNAME_FIELD_ID)));
        final WebElement useridInput = findElement(By.id(LOGIN_FORM_USERNAME_FIELD_ID));

        wait.until(ExpectedConditions.elementToBeClickable(useridInput));
        useridInput.clear();
        useridInput.sendKeys(userid);

        final WebElement pinInput = findElement(By.id(LOGIN_FORM_PASSWORD_FIELD_ID));
        wait.until(ExpectedConditions.elementToBeClickable(pinInput));
        pinInput.sendKeys(pin);

        final WebElement submitButton = findSubmitButtonByName(LOGIN_FORM_SUBMIT_BUTTON_NAME);
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
    private WebElement findSubmitButtonByName(String name) {
        List<WebElement> elements = findElements(By.name(name));
        if (elements == null || elements.size() != 1) {
            throw new RuntimeException("Submit button not found on the page");
        }

        return elements.get(0);
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

        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getOptions().setUseInsecureSSL(true); //ignore ssl certificate
        webClient.getOptions().setThrowExceptionOnScriptError(true);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

        try {
            //login
            HtmlPage loginPage = webClient.getPage(url);
            webClient.waitForBackgroundJavaScriptStartingBefore(200);
            webClient.waitForBackgroundJavaScript(20000);

            //get LoginForm
            loginPage.getEnclosingWindow().getJobManager().waitForJobs(10000);
            System.out.println(loginPage.asText());
            final HtmlForm form = loginPage.getFormByName("loginForm");
            form.getInputByName("username").type(userid);
            form.getInputByName("password").type(pin);
            final HtmlSubmitInput button = form.getInputByName("loginBtn");
            Page paymentPage = button.click();
            System.out.println(paymentPage.getUrl());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }


    public void quit() {
        super.quit();
    }
}
