package util.constant;

/**
 * Web elements names which are used in Sofortüberweisung verification site.
 */
public final class WebDriverSofortueberweisungConstants {

    /**
     * BIC input id
     */
    public static final String SU_LOGIN_BANK_CODE_SEARCH = "BankCodeSearch";

    /**
     * Account number (IBAN) input id
     */
    public static final String SU_LOGIN_NAME_ID = "BackendFormLOGINNAMEUSERID";

    /**
     * Account pin input id
     */
    public static final String SU_USER_PIN_ID = "BackendFormUSERPIN";

    /**
     * Verify TAN input id
     */
    public static final String SU_BACKEND_FORM_TAN = "BackendFormTan";

    /**
     * Name of radio button input with account to select for tests after log-in process.
     * <pre>
     * Girokonto (Max Mustermann)
     * DE06000000000023456789
     * </pre>
     */
    public static final String SU_TEST_ACCOUNT_RADIO_BUTTON = "account-1";

    public static final String SU_URL_SELECT_ACCOUNT_PATTERN = "select_account";
    public static final String SU_URL_PROVIDE_TAN_PATTERN = "provide_tan";
    public static final String SU_URL_PAY_1_PATTERN = "pay1";

    private WebDriverSofortueberweisungConstants() {
    }
}
