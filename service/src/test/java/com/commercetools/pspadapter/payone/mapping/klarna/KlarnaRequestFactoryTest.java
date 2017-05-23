package com.commercetools.pspadapter.payone.mapping.klarna;

import com.commercetools.pspadapter.BaseTenantPropertyTest;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
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
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class KlarnaRequestFactoryTest extends BaseTenantPropertyTest {

    private KlarnaRequestFactory klarnaRequestFactory;

    private PaymentWithCartLike paymentWithCartLike;

    private final PaymentTestHelper helper = new PaymentTestHelper();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        klarnaRequestFactory = new KlarnaRequestFactory(tenantConfig);
        paymentWithCartLike = helper.createKlarnaPaymentWithCartLike();
    }

    @Test
    public void createPreauthorizationRequest() throws Exception {
        SoftAssertions softly = new SoftAssertions();

        KlarnaPreauthorizationRequest request = klarnaRequestFactory.createPreauthorizationRequest(paymentWithCartLike);
        validateBaseRequestValues(request, RequestType.PREAUTHORIZATION.getType());

        // mandatory Klarna fields tested below
        softly.assertThat(request.getFirstname()).isEqualTo("Joana");
        softly.assertThat(request.getLastname()).isEqualTo("Smith");
        softly.assertThat(request.getStreet()).isEqualTo("666 blah-blah road");
        softly.assertThat(request.getZip()).isEqualTo("XXX YYY");
        softly.assertThat(request.getCity()).isEqualTo("Todmorder");
        softly.assertThat(request.getCountry()).isEqualTo("GB");
        softly.assertThat(request.getGender()).isEqualTo("f");
        softly.assertThat(request.getIp()).isEqualTo("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        softly.assertThat(request.getEmail()).isEqualTo("test.customer@test.co.uk");
        softly.assertThat(request.getTelephonenumber()).isEqualTo("+445566778899");
        softly.assertThat(request.getBirthday()).isEqualTo("19891203");
        softly.assertThat(request.getFinancingtype()).isEqualTo("KLV");
        softly.assertThat(request.getClearingtype()).isEqualTo("fnc");
        softly.assertThat(request.getLanguage()).isEqualTo("de");
        softly.assertThat(request.getAmount()).isEqualTo(24980);
        softly.assertThat(request.getCurrency()).isEqualTo("GBP");

        // the order of the items might be different,
        // but then below the order of the items' properties should be also changed
        softly.assertThat(request.getIt()).containsExactly(goods.toString(), goods.toString(), shipment.toString(), voucher.toString());
        softly.assertThat(request.getId()).containsExactly("4777200888", "4777300700", "DHL GB", "total discount");
        softly.assertThat(request.getPr()).containsExactly(4500L, 5900L, 950L, -2670L);

        softly.assertThat(request.getNo()).containsExactly(2L, 3L, 1L, 1L);

        softly.assertThat(request.getDe()).containsExactly("Ohrringe blau", "Armband blau", "shipping DHL GB", "total discount");
        softly.assertThat(request.getVa()).containsExactly(19, 19, 19, 0);

        // verify line items + shipment + discount == whole payment amount planned
        long sum = IntStream.range(0, 4).mapToLong(i -> request.getNo().get(i) * request.getPr().get(i)).sum();
        softly.assertThat(centAmountOf(paymentWithCartLike.getPayment().getAmountPlanned())).isEqualTo(sum);

        softly.assertAll();
    }

    @Test
    public void createAuthorizationRequest() throws Exception {
        assertThat("not implemented").isEqualTo("implemented");
    }

    @Test
    public void createRequestInternal() throws Exception {
        assertThat("not implemented").isEqualTo("implemented");
    }

}