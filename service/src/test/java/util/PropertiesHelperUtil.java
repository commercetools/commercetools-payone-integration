package util;

import java.io.InputStream;
import java.util.Properties;

public class PropertiesHelperUtil {

    private static final String TEST_INTERNAL_PROPERTIES = "test.internal.properties";
    private static final Properties testDataProperties = PropertiesHelperUtil.getTestDataConfig();

    public static Properties getTestDataConfig() {
        final Properties ctpCredentialsProperties = new Properties();
        try {
            final InputStream propStream =
                PropertiesHelperUtil.class.getClassLoader().getResourceAsStream(TEST_INTERNAL_PROPERTIES);
            if (propStream != null) {
                ctpCredentialsProperties.load(propStream);
                return ctpCredentialsProperties;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Test internal properties can't be read", exception);
        }

        return ctpCredentialsProperties;
    }

    public static String getTenant() {
        return testDataProperties.getProperty("tenant");
    }

    public static String getClientId() {
        return testDataProperties.getProperty("clientId");
    }

    public static String getProjectKey() {
        return testDataProperties.getProperty("projectKey");
    }

    public static String getClientSecret() {
        return testDataProperties.getProperty("clientSecret");
    }

    public static String getPayoneMerchantId() {
        return testDataProperties.getProperty("payoneMerchantId");
    }

    public static String getPayonePortalId() {
        return testDataProperties.getProperty("payonePortalId");
    }

    public static String getPayoneSubAccId() {
        return testDataProperties.getProperty("payoneSubAccId");
    }

    public static String getPayoneKey() {
        return testDataProperties.getProperty("payoneKey");
    }

    public static String getCardPan() {
        return testDataProperties.getProperty("cardPan");
    }
}
