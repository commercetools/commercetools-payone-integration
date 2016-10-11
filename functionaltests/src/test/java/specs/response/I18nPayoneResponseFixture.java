package specs.response;

import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.google.common.collect.ImmutableMap;
import org.concordion.api.FullOGNL;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Test the service returns localized error messages according to specified localization name in payment.
 * @author akovalenko
 */
@RunWith(ConcordionRunner.class)
@FullOGNL
public class I18nPayoneResponseFixture extends BasePaymentFixture {

    public String createCardPayment(String paymentName,
                                  String paymentMethod,
                                  String transactionType,
                                  String centAmount,
                                  String currencyCode,
                                  String languageCode) throws Exception {

        return createAndSaveCardPayment(paymentName, paymentMethod, transactionType, centAmount, currencyCode, languageCode);
    }

    public Map<String, Object> handleErrorJsonResponse(final String paymentName) throws ExecutionException, IOException {
        return ImmutableMap.of(
                "languageCode", fetchPaymentByLegibleName(paymentName).getCustom().getFieldAsString(CustomFieldKeys.LANGUAGE_CODE_FIELD),
                "jsonNode", handleJsonResponse(paymentName));
    }



}
