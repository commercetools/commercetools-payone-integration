// DON'T COMMIT
package com.commercetools.pspadapter.payone.domain.payone;

import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CreditCardAuthorizationRequest;
import com.commercetools.pspadapter.payone.transaction.TransactionBaseExecutor;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PayonePostServiceImplRequestTest extends BasePayonePostServiceImplTest {

    private static final String EN_ERROR_MESSAGE = "An error occured while processing this transaction (wrong parameters).";
    private static final String DE_ERROR_MESSAGE = "Bei der Bearbeitung dieser Transaktion ist ein Fehler aufgetreten (Falsche Parameter).";
    private static final String NL_ERROR_MESSAGE = "Er is bij de bewerking van deze transactie een fout opgetreden (verkeerde parameters).";

    // TODO: test factories, not only pure requests.
    // private CreditCardRequestFactory factory;

    @Before
    public void setUp() {
        super.setUp();
        // factory = new CreditCardRequestFactory(payoneConfig);
    }

    @Test
    public void nameI18nResponse() throws Exception {
        // it's a completely empty request, we expect error 1203 (Parameter {mid} faulty or missing) from payone
        CreditCardAuthorizationRequest request = new CreditCardAuthorizationRequest(payoneConfig, "NO MATTER");

        request.setLanguage("en");
        Map<String, String> result = payonePostService.executePost(request);
        assertThat(EN_ERROR_MESSAGE, is(result.get(TransactionBaseExecutor.CUSTOMER_MESSAGE)));

        request.setLanguage("de");
        result = payonePostService.executePost(request);
        assertThat(DE_ERROR_MESSAGE, is(result.get(TransactionBaseExecutor.CUSTOMER_MESSAGE)));

        request.setLanguage("nl");
        result = payonePostService.executePost(request);
        assertThat(NL_ERROR_MESSAGE, is(result.get(TransactionBaseExecutor.CUSTOMER_MESSAGE)));
    }


}