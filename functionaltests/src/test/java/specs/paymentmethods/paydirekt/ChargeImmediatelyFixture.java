package specs.paymentmethods.paydirekt;

import org.concordion.api.FullOGNL;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

@RunWith(ConcordionRunner.class)
@FullOGNL // required by containsSubstring() for redirect URL matching
public class ChargeImmediatelyFixture extends PaydirektFixture {

    // see base PaydirektFixture for implementation

}
