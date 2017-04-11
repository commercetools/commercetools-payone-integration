package specs.multitenancy;

import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

@RunWith(ConcordionRunner.class)
public class MultitenancyFixture extends BaseTenant2Fixture {

    /**
     * @return Second tenant project key
     */
    public String ctProject2Key() {
        return project2Key;
    }
}
