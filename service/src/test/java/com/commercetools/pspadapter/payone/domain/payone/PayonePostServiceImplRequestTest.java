// DON'T COMMIT
package com.commercetools.pspadapter.payone.domain.payone;

import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CreditCardAuthorizationRequest;
import com.commercetools.pspadapter.payone.transaction.TransactionBaseExecutor;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PayonePostServiceImplRequestTest extends BasePayonePostServiceImplTest {

    private static final String EN_ERROR_MESSAGE = "An error occured while processing this transaction (wrong parameters).";
    private static final String DE_ERROR_MESSAGE = "Bei der Bearbeitung dieser Transaktion ist ein Fehler aufgetreten (Falsche Parameter).";
    private static final String NL_ERROR_MESSAGE = "Er is bij de bewerking van deze transactie een fout opgetreden (verkeerde parameters).";

    @Test
    public void nameI18nResponse() throws Exception {
        // it's a completely empty request, we expect error 1203 (Parameter {mid} faulty or missing) from payone
        CreditCardAuthorizationRequest request = new CreditCardAuthorizationRequest(payoneConfig, "NO MATTER");

        request.setLanguage("xx");
        Map<String, String> result = payonePostService.executePost(request);
        final String defaultErrorMessage = result.get(TransactionBaseExecutor.CUSTOMER_MESSAGE);
        assertThat("With wrong local name we expect error 1203, but language is not guaranteed to be English",
                result.get(TransactionBaseExecutor.ERROR_CODE), is("1203"));

        request.setLanguage("");
        result = payonePostService.executePost(request);
        assertThat("With empty local name we expect error 1203, but language is not guaranteed to be English",
                result.get(TransactionBaseExecutor.ERROR_CODE), is("1203"));
        assertThat("With empty locale we should have the same error, as with wrong locale",
                result.get(TransactionBaseExecutor.CUSTOMER_MESSAGE), is(defaultErrorMessage));

        request.setLanguage(null);
        result = payonePostService.executePost(request);
        assertThat("With null locale name we expect error 1203, but language is not guaranteed to be English",
                result.get(TransactionBaseExecutor.ERROR_CODE), is("1203"));
        assertThat("With empty locale we should have the same error, as with wrong locale",
                result.get(TransactionBaseExecutor.CUSTOMER_MESSAGE), is(defaultErrorMessage));

        // test real locales
        request.setLanguage("en");
        result = payonePostService.executePost(request);
        assertThat("English error message is unexpected", result.get(TransactionBaseExecutor.CUSTOMER_MESSAGE), is(EN_ERROR_MESSAGE));

        request.setLanguage("de");
        result = payonePostService.executePost(request);
        assertThat("German error message is unexpected", result.get(TransactionBaseExecutor.CUSTOMER_MESSAGE), is(DE_ERROR_MESSAGE));

        request.setLanguage("nl");
        result = payonePostService.executePost(request);
        assertThat("Dutch error message is unexpected", result.get(TransactionBaseExecutor.CUSTOMER_MESSAGE), is(NL_ERROR_MESSAGE));
    }


}