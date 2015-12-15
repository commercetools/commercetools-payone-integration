package specs;

import com.google.common.base.Strings;

/**
 * @author fhaertig
 * @date 10.12.15
 */
public class BaseFixture {
    private static final String TEST_DATA_VISA_CREDIT_CARD_NO_3_DS = "TEST_DATA_VISA_CREDIT_CARD_NO_3DS";
    private static final String TEST_DATA_VISA_CREDIT_CARD_3_DS = "TEST_DATA_VISA_CREDIT_CARD_NO_3DS";

    public String getPayOneApiUrl() {
        return "https://api.pay1.de/post-gateway/";
    }

    public String getHandlePaymentUrl(final String paymentId) {
        return "http://localhost:8080/commercetools/handle/payment/" + paymentId;
    }

    protected String getGetConfigurationParameter(final String configParameterName) {
        final String envVariable = System.getenv(configParameterName);
        if (!Strings.isNullOrEmpty(envVariable)) {
            return envVariable;
        }

        final String sysProperty = System.getenv(configParameterName);
        if (!Strings.isNullOrEmpty(sysProperty)) {
            return sysProperty;
        }

        throw new RuntimeException(String.format(
                "Environment variable required for configuration must not be null or empty: %s",
                configParameterName));
    }

    protected String getUnconfirmedVisaPseudoCardPan() {
        return getGetConfigurationParameter(TEST_DATA_VISA_CREDIT_CARD_NO_3_DS);
    }

    protected String getVerifiedVisaPseudoCardPan() {
        return getGetConfigurationParameter(TEST_DATA_VISA_CREDIT_CARD_3_DS);
    }
}
