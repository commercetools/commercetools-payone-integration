package com.commercetools.pspadapter.payone.mapping.klarna;

import com.commercetools.pspadapter.BaseTenantPropertyTest;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaPreauthorizationRequest;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import util.PaymentTestHelper;

import java.util.stream.IntStream;

import static com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaItemTypeEnum.*;
import static io.sphere.sdk.utils.MoneyImpl.centAmountOf;

@RunWith(MockitoJUnitRunner.class)
public class KlarnaRequestFactoryTest extends BaseTenantPropertyTest {

    private KlarnaRequestFactory klarnaRequestFactory;

    private final PaymentTestHelper helper = new PaymentTestHelper();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        klarnaRequestFactory = new KlarnaRequestFactory(tenantConfig);
    }

    @Test
    public void createPreauthorizationRequest() throws Exception {
        SoftAssertions softly = new SoftAssertions();

        PaymentWithCartLike paymentWithCartLike = helper.createKlarnaPaymentWithCartLike();

        KlarnaPreauthorizationRequest request = klarnaRequestFactory.createPreauthorizationRequest(paymentWithCartLike);
        validateBaseRequestValues(request, RequestType.PREAUTHORIZATION.getType());

        // mandatory Klarna fields tested below
        softly.assertThat(request.getFirstname()).isEqualTo("Testperson-de");
        softly.assertThat(request.getLastname()).isEqualTo("Approved");
        softly.assertThat(request.getStreet()).isEqualTo("Hellersbergstraße 14");
        softly.assertThat(request.getZip()).isEqualTo("41460");
        softly.assertThat(request.getCity()).isEqualTo("Neuss");
        softly.assertThat(request.getCountry()).isEqualTo("DE");
        softly.assertThat(request.getGender()).isEqualTo("m");
        softly.assertThat(request.getIp()).isEqualTo("8.8.8.8");
        softly.assertThat(request.getEmail()).isEqualTo("youremail@email.com");
        softly.assertThat(request.getTelephonenumber()).isEqualTo("099776635674"); // value from payment should override value from address
        softly.assertThat(request.getBirthday()).isEqualTo("19881215");

        softly.assertThat(request.getFinancingtype()).isEqualTo("KLV");
        softly.assertThat(request.getClearingtype()).isEqualTo("fnc");
        softly.assertThat(request.getLanguage()).isEqualTo("de");
        softly.assertThat(request.getAmount()).isEqualTo(59685);
        softly.assertThat(request.getCurrency()).isEqualTo("EUR");

        // the order of the items might be different,
        // but then below the order of the items' properties should be also changed
        softly.assertThat(request.getIt()).containsExactly(goods.toString(), goods.toString(), goods.toString(), shipment.toString(), voucher.toString());
        softly.assertThat(request.getId()).containsExactly("123454323454667", "2345234523", "456786468866578", "DHL", "total discount");
        softly.assertThat(request.getPr()).containsExactly(12999L, 0L, 4923L, 495L, -7575L);

        softly.assertThat(request.getNo()).containsExactly(4L, 1L, 3L, 1L, 1L);

        softly.assertThat(request.getDe()).containsExactly("Halskette", "Kasten", "Ringe", "shipping DHL", "total discount");
        softly.assertThat(request.getVa()).containsExactly(19, 19, 19, 19, 0);

        // verify line items + shipment + discount == whole payment amount planned
        long sum = IntStream.range(0, request.getNo().size()).mapToLong(i -> request.getNo().get(i) * request.getPr().get(i)).sum();
        softly.assertThat(centAmountOf(paymentWithCartLike.getPayment().getAmountPlanned())).isEqualTo(sum);

        softly.assertAll();
    }

    @Test
    public void createAuthorizationRequest() throws Exception {
        SoftAssertions softly = new SoftAssertions();

        PaymentWithCartLike paymentWithCartLike = helper.createKlarnaPaymentWithCartLikeWithoutDiscount();

        KlarnaAuthorizationRequest request = klarnaRequestFactory.createAuthorizationRequest(paymentWithCartLike);
        validateBaseRequestValues(request, RequestType.AUTHORIZATION.getType());

        // mandatory Klarna fields tested below
        softly.assertThat(request.getFirstname()).isEqualTo("John");
        softly.assertThat(request.getLastname()).isEqualTo("Doe");
        softly.assertThat(request.getStreet()).isEqualTo("Hervamstr 666");
        softly.assertThat(request.getZip()).isEqualTo("81000");
        softly.assertThat(request.getCity()).isEqualTo("Munich");
        softly.assertThat(request.getCountry()).isEqualTo("DE");
        softly.assertThat(request.getGender()).isEqualTo("m");
        softly.assertThat(request.getIp()).isEqualTo("8.8.8.8");
        softly.assertThat(request.getEmail()).isEqualTo("aaa.bbb@ggg.de");
        softly.assertThat(request.getTelephonenumber()).isEqualTo("01234238746"); // value from payment should override value from address
        softly.assertThat(request.getBirthday()).isEqualTo("19591130");

        softly.assertThat(request.getFinancingtype()).isEqualTo("KLV");
        softly.assertThat(request.getClearingtype()).isEqualTo("fnc");
        softly.assertThat(request.getLanguage()).isEqualTo("en");
        softly.assertThat(request.getAmount()).isEqualTo(30900);
        softly.assertThat(request.getCurrency()).isEqualTo("EUR");

        // the order of the items might be different,
        // but then below the order of the items' properties should be also changed
        softly.assertThat(request.getIt()).containsExactly(goods.toString(), goods.toString(), goods.toString(), shipment.toString());
        softly.assertThat(request.getId()).containsExactly("123456", "776655", "998877665544", "DHL");

        // shipping price is 0, because shipping rate skipped on the carts over 15000
        softly.assertThat(request.getPr()).containsExactly(12900L, 0L, 4500L, 0L);

        softly.assertThat(request.getNo()).containsExactly(1L, 1L, 4L, 1L);

        softly.assertThat(request.getDe()).containsExactly("Necklace Swarovski", "Every piece", "Earrings", "shipping DHL");
        softly.assertThat(request.getVa()).containsExactly(19, 19, 19, 19);

        // verify line items + shipment + discount == whole payment amount planned
        long sum = IntStream.range(0, 4).mapToLong(i -> request.getNo().get(i) * request.getPr().get(i)).sum();
        softly.assertThat(centAmountOf(paymentWithCartLike.getPayment().getAmountPlanned())).isEqualTo(sum);

        softly.assertAll();
    }

}