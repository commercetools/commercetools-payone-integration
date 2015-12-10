package specs;

/**
 * @author fhaertig
 * @date 10.12.15
 */
public class BaseFixture {
    public String getPayOneApiUrl() {
        return "https://api.pay1.de/post-gateway/";
    }

    public String getHandlePaymentUrl(final String paymentId) {
        return "http://localhost:8080/commercetools/handle/payment/" + paymentId;
    }
}
