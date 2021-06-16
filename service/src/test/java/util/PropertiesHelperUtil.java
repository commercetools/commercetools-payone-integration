package util;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

public class PropertiesHelperUtil {

    private static final String TEST_INTERNAL_PROPERTIES = "test.internal.properties";
    private static final Properties testDataProperties = PropertiesHelperUtil.getTestDataConfig();
    public static final String TEST_DATA_CT_CLIENT_ID = "TEST_DATA_CT_CLIENT_ID";
    public static final String TEST_DATA_CT_CLIENT_SECRET = "TEST_DATA_CT_CLIENT_SECRET";
    public static final String TEST_DATA_CT_PROJECT_KEY = "TEST_DATA_CT_PROJECT_KEY";
    public static final String TEST_DATA_PAYONE_MERCHANT_ID = "TEST_DATA_PAYONE_MERCHANT_ID";
    public static final String TEST_DATA_PAYONE_SUBACC_ID = "TEST_DATA_PAYONE_SUBACC_ID";
    public static final String TEST_DATA_PAYONE_PORTAL_ID = "TEST_DATA_PAYONE_PORTAL_ID";
    public static final String TEST_DATA_PAYONE_KEY = "TEST_DATA_PAYONE_KEY";
    public static final String TEST_DATA_TENANT_NAME = "TEST_DATA_TENANT_NAME";
    public static final String TEST_DATA_VISA_CREDIT_CARD_NO_3DS = "TEST_DATA_VISA_CREDIT_CARD_NO_3DS";

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

        return loadEnvironmentVariables(ctpCredentialsProperties);
    }

    private static Properties loadEnvironmentVariables(Properties ctpCredentialsProperties) {

        final Map<String, String> environmentVariables = System.getenv();
        ctpCredentialsProperties.putAll(environmentVariables);
//        ctpCredentialsProperties.setProperty(TEST_DATA_CT_CLIENT_ID, environmentVariables.get(TEST_DATA_CT_CLIENT_ID));
//        ctpCredentialsProperties.setProperty(TEST_DATA_CT_CLIENT_SECRET, environmentVariables.get(TEST_DATA_CT_CLIENT_SECRET));
//        ctpCredentialsProperties.setProperty(TEST_DATA_CT_PROJECT_KEY, environmentVariables.get(TEST_DATA_CT_PROJECT_KEY));
//        ctpCredentialsProperties.setProperty(TEST_DATA_PAYONE_MERCHANT_ID, environmentVariables.get(TEST_DATA_PAYONE_MERCHANT_ID));
//        ctpCredentialsProperties.setProperty(TEST_DATA_PAYONE_SUBACC_ID, environmentVariables.get(TEST_DATA_PAYONE_SUBACC_ID));
//        ctpCredentialsProperties.setProperty(TEST_DATA_PAYONE_PORTAL_ID, environmentVariables.get(TEST_DATA_PAYONE_PORTAL_ID));
//        ctpCredentialsProperties.setProperty(TEST_DATA_PAYONE_KEY, environmentVariables.get(TEST_DATA_PAYONE_KEY));
//        ctpCredentialsProperties.setProperty(TEST_DATA_TENANT_NAME, environmentVariables.get(TEST_DATA_TENANT_NAME));
//        ctpCredentialsProperties.setProperty(TEST_DATA_VISA_CREDIT_CARD_NO_3DS, environmentVariables.get(TEST_DATA_VISA_CREDIT_CARD_NO_3DS));

        return ctpCredentialsProperties;
    }

    public static String getTenant() {
        return testDataProperties.getProperty(TEST_DATA_TENANT_NAME);
    }

    public static String getClientId() {
        return testDataProperties.getProperty(TEST_DATA_CT_CLIENT_ID);
    }

    public static String getProjectKey() {
        return testDataProperties.getProperty(TEST_DATA_CT_PROJECT_KEY);
    }

    public static String getClientSecret() {
        return testDataProperties.getProperty(TEST_DATA_CT_CLIENT_SECRET);
    }

    public static String getPayoneMerchantId() {
        return testDataProperties.getProperty(TEST_DATA_PAYONE_MERCHANT_ID);
    }

    public static String getPayonePortalId() {
        return testDataProperties.getProperty(TEST_DATA_PAYONE_PORTAL_ID);
    }

    public static String getPayoneSubAccId() {
        return testDataProperties.getProperty(TEST_DATA_PAYONE_SUBACC_ID);
    }

    public static String getPayoneKey() {
        return testDataProperties.getProperty(TEST_DATA_PAYONE_KEY);
    }

    public static String getCardPan() {
        return testDataProperties.getProperty(TEST_DATA_VISA_CREDIT_CARD_NO_3DS);
    }
}
