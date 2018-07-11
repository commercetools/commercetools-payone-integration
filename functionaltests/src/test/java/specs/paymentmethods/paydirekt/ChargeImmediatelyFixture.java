package specs.paymentmethods.paydirekt;

import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.google.common.collect.ImmutableList;
import io.sphere.sdk.payments.Payment;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.concordion.api.FullOGNL;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.WebDriverPaydirekt;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;

@RunWith(ConcordionRunner.class)
@FullOGNL // required by containsSubstring() for redirect URL matching
public class ChargeImmediatelyFixture extends PaydirektFixture {


    private static final String baseRedirectUrl = "https://www.example.com/paydirekt_charge_immediately/";
    private static Logger LOG = LoggerFactory.getLogger(specs.paymentmethods.paydirekt.ChargeImmediatelyFixture.class);
    private Map<String, String> successUrlForPayment;

    public boolean executeRedirectForPayments(final String paymentNames) throws ExecutionException {
        final Collection<String> paymentNamesList = ImmutableList.copyOf(thePaymentNamesSplitter.split(paymentNames));


        successUrlForPayment = paymentNamesList.stream()
                .map(this::approvePaymentAsCustomer)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toMap(Pair::getKey, Pair::getValue));

        return successUrlForPayment.size() == paymentNamesList.size();
    }

    /**
     * Simulate customer's payment approval: log in in browser, set field values, press submit buttons.
     *
     * @param paymentName payment name to approve
     * @return optional {@code paymentName -> successUrl} pairs if payment has been approved succesfuly.
     */
    private Optional<Pair<String, String>> approvePaymentAsCustomer(String paymentName) {
        final Payment payment = fetchPaymentByLegibleName(paymentName);
        final WebDriverPaydirekt webDriver = new WebDriverPaydirekt();
        try {
            return Optional.ofNullable(payment.getCustom())
                    .map(customFields -> customFields.getFieldAsString(CustomFieldKeys.REDIRECT_URL_FIELD))
                    .map(redirectCustomField -> webDriver.executePayDirectRedirect(redirectCustomField,
                            getTestDataPaydirectLogin(),
                            getTestDataPaydirectPin())
                            .replace(baseRedirectUrl, "[...]"))
                    .map(successUrl -> Pair.of(paymentName, successUrl));

        } catch (Exception e) {
            LOG.error("Error redirect for Paydirect Charge Immediate for payment name [{}], id = [{}]",
                    paymentName, payment.getId(), e);
        } finally {
            webDriver.deleteCookies();
            webDriver.quit();
        }

        return empty();
    }

    @Override
    public boolean receivedNotificationOfActionFor(final String paymentNames, final String txaction) throws Exception {
        // validate the payments were successfully processed in previous executeRedirectForPayments() call.
        // otherwise return false instantly
        String unconfirmedPayments = stream(thePaymentNamesSplitter.split(paymentNames).spliterator(), false)
                .filter(paymentName -> !successUrlForPayment.containsKey(paymentName))
                .collect(Collectors.joining(", "));

        if (StringUtils.isNotBlank(unconfirmedPayments)) {
            LOG.error("[{}] payments are not re-directed - the notifications [{}] won't come",
                    unconfirmedPayments, txaction);
            return false;
        }

        return super.receivedNotificationOfActionFor(paymentNames, txaction);
    }

    @Override
    public boolean receivedNextNotificationOfActionFor(String paymentNames, String txaction, String prevTxaction)
            throws Exception {
        // we keep this overriding just to easily see which test methods are run in this fixture
        return super.receivedNextNotificationOfActionFor(paymentNames, txaction, prevTxaction);
    }

}
