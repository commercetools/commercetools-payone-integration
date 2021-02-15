package util;


import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.Optional;

import static util.constant.WebDriverCommon.CLASS_NAME_PRIMARY;
import static util.constant.WebDriverSofortueberweisungConstants.SU_BACKEND_FORM_TAN;
import static util.constant.WebDriverSofortueberweisungConstants.SU_LOGIN_BANK_CODE_SEARCH;
import static util.constant.WebDriverSofortueberweisungConstants.SU_LOGIN_NAME_ID;
import static util.constant.WebDriverSofortueberweisungConstants.SU_TEST_ACCOUNT_RADIO_BUTTON;
import static util.constant.WebDriverSofortueberweisungConstants.SU_USER_PIN_ID;

/**
 * @author fhaertig
 * @since 14.03.16
 */
public class WebDriverSofortueberweisung extends CustomWebDriver {


    private static final String SU_MODAL_CONTENT_CSS_CLASS = "modal-content";
    private static final String SU_COOKIE_MODAL_BASIC_HTML_ID = "cookie-modal-basic";
    public static final String SU_ACCEPT_COOKIES_CSS_CLASS = "cookie-modal-accept-all";

    public WebDriverSofortueberweisung() {
        super();

    }

    private void doLogin(String userid, String pin) {
        final WebElement useridInput = findElement(By.id(SU_LOGIN_NAME_ID));

        useridInput.clear();
        useridInput.sendKeys(userid);

        final WebElement pinInput = findElement(By.id(SU_USER_PIN_ID));
        pinInput.sendKeys(pin);

        final WebElement submitButton = findSubmitButton();
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
        List<WebElement> elements = getDriver().findElements(By.className(CLASS_NAME_PRIMARY));
        if (elements == null || elements.size() != 1) {
            throw new RuntimeException("Submit button not found on the page");
        }

        return elements.get(0);
    }

    private void selectRegion(final String bic) {
        Optional<WebElement> acceptCookieWebElement = Optional.ofNullable(findElement(By.className(SU_MODAL_CONTENT_CSS_CLASS)))
                .map(modalContentElement -> modalContentElement.findElements(By.id(SU_COOKIE_MODAL_BASIC_HTML_ID)))
                .map(listOfElements -> listOfElements.get(0))
                .map(cookieDivElement -> cookieDivElement.findElement(By.className(SU_ACCEPT_COOKIES_CSS_CLASS)));
        acceptCookieWebElement.ifPresent(WebElement::click);

        final WebElement bankCodeSearchInput = findElement(By.id(SU_LOGIN_BANK_CODE_SEARCH));
        bankCodeSearchInput.clear();
        bankCodeSearchInput.sendKeys(bic);

        final WebElement submitButton = findSubmitButton();
        submitButton.click();
    }


    private void selectAccount() {
        final WebElement senderAccountInput = findElement(By.id(SU_TEST_ACCOUNT_RADIO_BUTTON));

        senderAccountInput.click();

        final WebElement submitButton = findSubmitButton();
        submitButton.click();
    }

    private void provideTan(String tan) {
        final WebElement tanInput = findElement(By.id(SU_BACKEND_FORM_TAN));
        final WebElement submitButton = findSubmitButton();

        tanInput.sendKeys(tan);

        submitButton.click();
    }

    /**
     * Submits the given {@code password} at the given {@code url}'s "password" element, waits for a redirect and
     * returns the URL it was redirected to.
     *
     * @param url       the URL to navigate to
     * @param bic       the bank code / BIC to use
     * @param userid    the account id to use
     * @param pin       the pin to login
     * @param tan       the tan to use
     * @return the URL the browser was redirected to after submitting the {@code password}
     */
    public String executeSofortueberweisungRedirect(final String url, final String bic, final String userid,
                                                    final String pin, final String tan) {
        getDriver().get(url);
        selectRegion(bic);
        doLogin(userid, pin);
        selectAccount();

        provideTan(tan);
        WebDriverWait wait = new WebDriverWait(getDriver(), 10);
        Boolean waitToSuccess = wait.until(ExpectedConditions.urlContains("-Success"));
        return waitToSuccess ? getUrl() : "";
}


}

