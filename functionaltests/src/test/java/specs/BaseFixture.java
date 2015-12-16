package specs;

import com.google.common.base.Strings;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;

import java.io.IOException;
import java.net.URI;
import java.util.Random;

/**
 * @author fhaertig
 * @date 10.12.15
 */
public class BaseFixture {
    private static final String TEST_DATA_VISA_CREDIT_CARD_NO_3_DS = "TEST_DATA_VISA_CREDIT_CARD_NO_3DS";
    private static final String TEST_DATA_VISA_CREDIT_CARD_3_DS = "TEST_DATA_VISA_CREDIT_CARD_NO_3DS";

    private static final Random randomSource = new Random();

    public String getPayOneApiUrl() {
        return "https://api.pay1.de/post-gateway/";
    }

    public String getHandlePaymentUrl(final String paymentId) {
        try {
            //used for external address
            String url = getGetConfigurationParameter("HANDLE_URL");
            return url + paymentId;
        } catch (Exception ex) {
            //default local address
            return "http://localhost:8080/commercetools/handle/payment/" + paymentId;
        }
    }

    public HttpResponse sendGetRequestToUrl(final String url) throws IOException {
        return Request.Get(url)
                .connectTimeout(200)
                .execute()
                .returnResponse();
    }

    protected String getGetConfigurationParameter(final String configParameterName) {
        final String envVariable = System.getenv(configParameterName);
        if (!Strings.isNullOrEmpty(envVariable)) {
            return envVariable;
        }

        final String sysProperty = System.getProperty(configParameterName);
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

    protected String getRandomOrderNumber() {
        return String.valueOf(randomSource.nextInt() + System.currentTimeMillis());
    }
}

