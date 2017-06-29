package model;

import io.sphere.sdk.payments.Payment;
import org.apache.http.HttpResponse;

public class HandlePaymentResult {
    private final HttpResponse httpResponse;
    private final Payment payment;

    public HandlePaymentResult(HttpResponse httpResponse, Payment payment) {
        this.httpResponse = httpResponse;
        this.payment = payment;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public Payment getPayment() {
        return payment;
    }
}
