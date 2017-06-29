package specs.scheduler;

import org.concordion.api.MultiValueResult;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import specs.multitenancy.BaseTenant2Fixture;

@RunWith(ConcordionRunner.class)
public class ScheduledJobShort2Fixture extends BaseTenant2Fixture {

    private ScheduledJobFixtureHelper helper;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        helper = new ScheduledJobFixtureHelper(this);
    }

    public MultiValueResult createPayment(String paymentName,
                                          String paymentMethod,
                                          String transactionType,
                                          String centAmount,
                                          String currencyCode) throws Exception {

        return helper.createPayment(paymentName, paymentMethod, transactionType, centAmount, currencyCode);
    }

    public static int waitSecondsTimeout() {
        return ScheduledJobFixtureHelper.waitSecondsTimeout();
    }


    public MultiValueResult verifyPaymentIsHandled(String paymentName) throws Exception {
        return helper.verifyPaymentIsHandled(paymentName);
    }
}
