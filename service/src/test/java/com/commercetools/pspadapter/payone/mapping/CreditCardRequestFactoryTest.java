package com.commercetools.pspadapter.payone.mapping;

import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CreditCardCaptureRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CreditCardPreauthorizationRequest;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.payments.Payment;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.javamoney.moneta.function.MonetaryUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import util.PaymentTestHelper;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * @author fhaertig
 * @date 14.12.15
 */
@RunWith(MockitoJUnitRunner.class)
public class CreditCardRequestFactoryTest {

    private final PaymentTestHelper payments = new PaymentTestHelper();
    private CreditCardRequestFactory factory;

    @Mock
    private PropertyProvider propertyProvider;

    private PayoneConfig config;

    @Before
    public void setUp() {
        when(propertyProvider.getProperty(any())).thenReturn(Optional.of("dummyValue"));
        when(propertyProvider.getMandatoryNonEmptyProperty(any())).thenReturn("dummyValue");

        config = new ServiceConfig(propertyProvider).getPayoneConfig();
        factory = new CreditCardRequestFactory(config);
    }

    @Test
    public void throwExceptionWhenPaymentIncomplete() throws Exception {
        Payment payment = payments.dummyPaymentNoCustomFields();
        Order order = payments.dummyOrderMapToPayoneRequest();
        final Throwable throwable = catchThrowable(() -> factory.createPreauthorizationRequest(new PaymentWithCartLike(payment, order)));

        Assertions.assertThat(throwable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing custom fields on payment!");
    }

    @Test
    public void createFullPreauthorizationRequestFromValidPayment() throws Exception {

        Payment payment = payments.dummyPaymentOneAuthPending20Euro();
        Order order = payments.dummyOrderMapToPayoneRequest();
        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, order);
        CreditCardPreauthorizationRequest result = factory.createPreauthorizationRequest(paymentWithCartLike);
        SoftAssertions softly = new SoftAssertions();

        //base values
        softly.assertThat(result.getRequest()).isEqualTo(RequestType.PREAUTHORIZATION.getType());
        softly.assertThat(result.getAid()).isEqualTo(config.getSubAccountId());
        softly.assertThat(result.getMid()).isEqualTo(config.getMerchantId());
        softly.assertThat(result.getPortalid()).isEqualTo(config.getPortalId());
        softly.assertThat(result.getKey()).isEqualTo(config.getKeyAsMd5Hash());
        softly.assertThat(result.getMode()).isEqualTo(config.getMode());
        softly.assertThat(result.getApiVersion()).isEqualTo(config.getApiVersion());

        //clearing type
        String clearingType = ClearingType.getClearingTypeByKey(payment.getPaymentMethodInfo().getMethod()).getPayoneCode();
        softly.assertThat(result.getClearingtype()).isEqualTo(clearingType);

        //references
        softly.assertThat(result.getReference()).isEqualTo(paymentWithCartLike.getOrderNumber().get());
        softly.assertThat(result.getCustomerid()).isEqualTo(payment.getCustomer().getObj().getCustomerNumber());

        //monetary
        softly.assertThat(result.getAmount()).isEqualTo(MonetaryUtil.minorUnits().queryFrom(payment.getAmountPlanned()).intValue());
        softly.assertThat(result.getCurrency()).isEqualTo(payment.getAmountPlanned().getCurrency().getCurrencyCode());

        //3d secure
        softly.assertThat(result.getEcommercemode()).isEqualTo(payment.getCustom().getFieldAsBoolean(CustomFieldKeys.FORCE3DSECURE_KEY));

        //address data
        Address billingAddress = order.getBillingAddress();
        softly.assertThat(result.getTitle()).isEqualTo(billingAddress.getTitle());
        softly.assertThat(result.getSalutation()).isEqualTo(billingAddress.getSalutation());
        softly.assertThat(result.getFirstname()).isEqualTo(billingAddress.getFirstName());
        softly.assertThat(result.getLastname()).isEqualTo(billingAddress.getLastName());
        softly.assertThat(result.getBirthday()).isEqualTo(payment.getCustomer().getObj().getDateOfBirth().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        softly.assertThat(result.getCompany()).isEqualTo(billingAddress.getCompany());
        softly.assertThat(result.getStreet()).isEqualTo(billingAddress.getStreetName() + " " + billingAddress.getStreetNumber());
        softly.assertThat(result.getAddressaddition()).isEqualTo(billingAddress.getAdditionalStreetInfo());
        softly.assertThat(result.getCity()).isEqualTo(billingAddress.getCity());
        softly.assertThat(result.getZip()).isEqualTo(order.getBillingAddress().getPostalCode());
        softly.assertThat(result.getCountry()).isEqualTo(order.getBillingAddress().getCountry().toLocale().getCountry());
        softly.assertThat(result.getEmail()).isEqualTo(order.getBillingAddress().getEmail());
        softly.assertThat(result.getTelephonenumber()).isEqualTo(Optional
                .ofNullable(billingAddress.getPhone())
                .orElse(billingAddress.getMobile()));

        //TODO: need to check also
        //gender
        //billingAddress.state
        //shippingAddress

        softly.assertAll();
    }

    @Test
    public void createFullCaptureRequestFromValidPayment() throws Exception {

        Payment payment = payments.dummyPaymentOneAuthPending20Euro();
        Order order = payments.dummyOrderMapToPayoneRequest();
        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, order);
        CreditCardCaptureRequest result = factory.createCaptureRequest(paymentWithCartLike, payment.getTransactions().get(0));
        SoftAssertions softly = new SoftAssertions();

        //base values
        softly.assertThat(result.getRequest()).isEqualTo(RequestType.CAPTURE.getType());
        softly.assertThat(result.getMid()).isEqualTo(config.getMerchantId());
        softly.assertThat(result.getPortalid()).isEqualTo(config.getPortalId());
        softly.assertThat(result.getKey()).isEqualTo(config.getKeyAsMd5Hash());
        softly.assertThat(result.getMode()).isEqualTo(config.getMode());
        softly.assertThat(result.getApiVersion()).isEqualTo(config.getApiVersion());

        //monetary
        softly.assertThat(result.getAmount()).isEqualTo(MonetaryUtil.minorUnits().queryFrom(payment.getAmountAuthorized()).intValue());
        softly.assertThat(result.getCurrency()).isEqualTo(payment.getAmountAuthorized().getCurrency().getCurrencyCode());

        softly.assertThat(result.getTxid()).isEqualTo(payment.getInterfaceId());
        softly.assertThat(result.getSequencenumber()).isEqualTo(Integer.valueOf(payment.getTransactions().get(0).getInteractionId()));

        softly.assertAll();
    }


}