package com.commercetools.pspadapter.payone.mapping.klarna;

import com.commercetools.pspadapter.BaseTenantPropertyTest;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneRequestWithCart;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaPreauthorizationRequest;
import com.commercetools.pspadapter.payone.mapping.CountryToLanguageMapper;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.payments.Payment;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import util.PaymentTestHelper;

import java.util.Locale;
import java.util.stream.IntStream;

import static com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaItemTypeEnum.*;
import static com.commercetools.pspadapter.payone.mapping.klarna.KlarnaRequestFactory.KLARNA_AUTHORIZATION_TOKEN;
import static com.neovisionaries.i18n.CountryCode.*;
import static io.sphere.sdk.utils.MoneyImpl.centAmountOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KlarnaRequestFactoryTest extends BaseTenantPropertyTest {

    private KlarnaRequestFactory klarnaRequestFactory;

    private final PaymentTestHelper helper = new PaymentTestHelper();

    private final CountryToLanguageMapper countryToLanguageMapper = new PayoneKlarnaCountryToLanguageMapper();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        klarnaRequestFactory = new KlarnaRequestFactory(tenantConfig, countryToLanguageMapper);
    }

    @Test
    public void createPreauthorizationRequest() throws Exception {
        SoftAssertions softly = new SoftAssertions();

        PaymentWithCartLike paymentWithCartLike = helper.createKlarnaPaymentWithCartLike();

        KlarnaPreauthorizationRequest request = klarnaRequestFactory.createPreauthorizationRequest(paymentWithCartLike);
        validateBaseRequestValues(request, RequestType.PREAUTHORIZATION.getType());

        // mandatory Klarna fields tested below
        softly.assertThat(request.getPayData().get(KLARNA_AUTHORIZATION_TOKEN)).isEqualTo("testAuthorizationToken");
        softly.assertThat(request.getFirstname()).isEqualTo("Testperson-de");
        softly.assertThat(request.getLastname()).isEqualTo("Approved");
        softly.assertThat(request.getStreet()).isEqualTo("Hellersbergstraße 14");
        softly.assertThat(request.getZip()).isEqualTo("41460");
        softly.assertThat(request.getCity()).isEqualTo("Neuss");
        softly.assertThat(request.getCountry()).isEqualTo("DE");
        softly.assertThat(request.getGender()).isEqualTo("m");
        softly.assertThat(request.getIp()).isEqualTo("8.8.8.8");
        softly.assertThat(request.getEmail()).isEqualTo("youremail@email.com");
        softly.assertThat(request.getTelephonenumber()).isEqualTo("01234238746"); // value from payment should override value from address
        softly.assertThat(request.getBirthday()).isEqualTo("19881215");

        softly.assertThat(request.getFinancingtype()).isEqualTo("KIV");
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
        softly.assertThat(request.getPayData().get(KLARNA_AUTHORIZATION_TOKEN)).isEqualTo("testAuthorizationToken");
        softly.assertThat(request.getFirstname()).isEqualTo("John");
        softly.assertThat(request.getLastname()).isEqualTo("Doe");
        softly.assertThat(request.getStreet()).isEqualTo("Hervamstr 666");
        softly.assertThat(request.getZip()).isEqualTo("81000");
        softly.assertThat(request.getCity()).isEqualTo("Munich");
        softly.assertThat(request.getCountry()).isEqualTo("DE");
        softly.assertThat(request.getGender()).isEqualTo("m");
        softly.assertThat(request.getIp()).isEqualTo("8.8.8.8");
        softly.assertThat(request.getEmail()).isEqualTo("aaa.bbb@ggg.de");
        softly.assertThat(request.getTelephonenumber()).isEqualTo("099776635674"); // value from payment should override value from address
        softly.assertThat(request.getBirthday()).isEqualTo("19591130");

        softly.assertThat(request.getFinancingtype()).isEqualTo("KIV");
        softly.assertThat(request.getClearingtype()).isEqualTo("fnc");
        softly.assertThat(request.getLanguage()).isEqualTo("de"); // the language for Klarna is mapped from country, even if it is explicitly set in the cart
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

    @Test
    public void countryToLanguageMapping_common() throws Exception {
        SoftAssertions softly = new SoftAssertions();

        Payment payment = paymentsTestHelper.dummyPaymentNoTransaction20EuroPlanned();
        CartLike cartLike = mock(CartLike.class);
        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, cartLike);
        PayoneRequestWithCart request = klarnaRequestFactory.createPreauthorizationRequest(paymentWithCartLike);

        softly.assertThat(request.getCountry()).isNull();
        softly.assertThat(request.getLanguage()).isNull();

        when(cartLike.getBillingAddress()).thenReturn(Address.of(DE));
        request = klarnaRequestFactory.createPreauthorizationRequest(paymentWithCartLike);
        softly.assertThat(request.getCountry()).isEqualTo("DE");
        softly.assertThat(request.getLanguage()).isEqualTo("de");

        when(cartLike.getBillingAddress()).thenReturn(Address.of(NL));
        request = klarnaRequestFactory.createPreauthorizationRequest(paymentWithCartLike);
        softly.assertThat(request.getCountry()).isEqualTo("NL");
        softly.assertThat(request.getLanguage()).isEqualTo("nl");

        when(cartLike.getBillingAddress()).thenReturn(Address.of(UK));
        request = klarnaRequestFactory.createPreauthorizationRequest(paymentWithCartLike);
        softly.assertThat(request.getCountry()).isEqualTo("GB");
        softly.assertThat(request.getLanguage()).isNull();

        // verify language falls back to cart.locale if mapping is not possible from country
        when(cartLike.getBillingAddress()).thenReturn(Address.of(US));
        when(cartLike.getLocale()).thenReturn(Locale.FRANCE);
        request = klarnaRequestFactory.createPreauthorizationRequest(paymentWithCartLike);
        softly.assertThat(request.getCountry()).isEqualTo("US");
        softly.assertThat(request.getLanguage()).isEqualTo("fr");

        when(cartLike.getBillingAddress()).thenReturn(null);
        when(cartLike.getLocale()).thenReturn(Locale.CHINESE);
        request = klarnaRequestFactory.createPreauthorizationRequest(paymentWithCartLike);
        softly.assertThat(request.getCountry()).isNull();
        softly.assertThat(request.getLanguage()).isEqualTo("zh");

        // no country and language
        when(cartLike.getBillingAddress()).thenReturn(null);
        when(cartLike.getLocale()).thenReturn(null);
        request = klarnaRequestFactory.createPreauthorizationRequest(paymentWithCartLike);
        softly.assertThat(request.getCountry()).isNull();
        softly.assertThat(request.getLanguage()).isNull();

        // language from country has precedence for Klarna: locale is fi, but Austria mapped to de
        when(cartLike.getBillingAddress()).thenReturn(Address.of(AT));
        when(cartLike.getLocale()).thenReturn(new Locale("fi"));
        request = klarnaRequestFactory.createPreauthorizationRequest(paymentWithCartLike);
        softly.assertThat(request.getCountry()).isEqualTo("AT");
        softly.assertThat(request.getLanguage()).isEqualTo("de");

        // if country language is not mappable in Klarna - locale from cart is used
        when(cartLike.getBillingAddress()).thenReturn(Address.of(UA));
        when(cartLike.getLocale()).thenReturn(new Locale("fi"));
        request = klarnaRequestFactory.createPreauthorizationRequest(paymentWithCartLike);
        softly.assertThat(request.getCountry()).isEqualTo("UA");
        softly.assertThat(request.getLanguage()).isEqualTo("fi");

        softly.assertAll();
    }

    @Test
    public void countryToLanguageMapping_fromShippingAddress() throws Exception {
        SoftAssertions softly = new SoftAssertions();

        Payment payment = paymentsTestHelper.dummyPaymentNoTransaction20EuroPlanned();
        CartLike cartLike = mock(CartLike.class);
        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, cartLike);
        PayoneRequestWithCart request;

        when(cartLike.getShippingAddress()).thenReturn(Address.of(DE));
        request = klarnaRequestFactory.createPreauthorizationRequest(paymentWithCartLike);
        softly.assertThat(request.getLanguage()).isEqualTo("de");

        when(cartLike.getShippingAddress()).thenReturn(Address.of(NL));
        request = klarnaRequestFactory.createPreauthorizationRequest(paymentWithCartLike);
        softly.assertThat(request.getLanguage()).isEqualTo("nl");

        // shipping address is set, but the country language can't be mapped
        when(cartLike.getShippingAddress()).thenReturn(Address.of(CH));
        request = klarnaRequestFactory.createPreauthorizationRequest(paymentWithCartLike);
        softly.assertThat(request.getLanguage()).isNull();

        softly.assertAll();
    }

    @Test
    public void countryToLanguageMapping_fromBothAddress() throws Exception {
        SoftAssertions softly = new SoftAssertions();

        Payment payment = paymentsTestHelper.dummyPaymentNoTransaction20EuroPlanned();
        CartLike cartLike = mock(CartLike.class);
        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, cartLike);
        PayoneRequestWithCart request;

        when(cartLike.getBillingAddress()).thenReturn(Address.of(AT));
        when(cartLike.getShippingAddress()).thenReturn(Address.of(NL));
        request = klarnaRequestFactory.createPreauthorizationRequest(paymentWithCartLike);
        softly.assertThat(request.getLanguage()).isEqualTo("de");

        when(cartLike.getBillingAddress()).thenReturn(Address.of(NL));
        when(cartLike.getShippingAddress()).thenReturn(Address.of(AT));
        request = klarnaRequestFactory.createPreauthorizationRequest(paymentWithCartLike);
        softly.assertThat(request.getLanguage()).isEqualTo("nl");


        softly.assertAll();
    }

    @Test
    public void dateOfBirthRemainsFromCustomerIfMissingInCustomFields() throws Exception {
        Payment payment = paymentsTestHelper.dummyPaymentNoTransaction20EuroPlanned();
        CartLike cartLike = mock(CartLike.class);
        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, cartLike);
        PayoneRequestWithCart request = klarnaRequestFactory.createPreauthorizationRequest(paymentWithCartLike);

        assertThat(request.getBirthday()).isEqualTo("19891203"); // from customer.obj.dateOfBirth
    }
}
