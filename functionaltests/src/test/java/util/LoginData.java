package util;

/**
 * @author fhaertig
 * @since 31.01.16
 */
public class LoginData {

    private final String accountIdentifier;

    private final String password;

    public LoginData(final String accountIdentifier, final String password) {
        this.accountIdentifier = accountIdentifier;
        this.password = password;
    }

    public String getAccountIdentifier() {
        return accountIdentifier;
    }

    public String getPassword() {
        return password;
    }

}
