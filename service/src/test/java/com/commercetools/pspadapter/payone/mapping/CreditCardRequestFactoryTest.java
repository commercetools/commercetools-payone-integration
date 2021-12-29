package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.BaseTenantPropertyTest;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CreditCardPayoneRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CreditCardCaptureRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CreditCardPreauthorizationRequest;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.payments.Payment;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import util.PaymentTestHelper;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaItemTypeEnum.*;
import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.LANGUAGE_CODE_FIELD;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.javamoney.moneta.function.MonetaryQueries.convertMinorPart;

/**
 * @author fhaertig
 * @since 14.12.15
 */
@RunWith(MockitoJUnitRunner.class)
public class CreditCardRequestFactoryTest extends BaseTenantPropertyTest {

    private final PaymentTestHelper payments = new PaymentTestHelper();
    private CreditCardRequestFactory factory;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        factory = new CreditCardRequestFactory(tenantConfig);
    }

    @Test
    public void throwExceptionWhenPaymentIncomplete() throws Exception {
        Payment payment = payments.dummyPaymentNoCustomFields();
        Order order = payments.dummyOrderMapToPayoneRequest();
        final Throwable throwable = catchThrowable(() -> factory.createPreauthorizationRequest(new PaymentWithCartLike(payment, order)));

        Assertions.assertThat(throwable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is missing the required custom field");
    }

    @Test
    public void createFullPreauthorizationRequestFromValidPayment() throws Exception {

        Payment payment = payments.dummyPaymentOneAuthPending20EuroCC();
        Order order = payments.dummyOrderMapToPayoneRequest();
        Customer customer = payment.getCustomer().getObj();
        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, order);
        CreditCardPreauthorizationRequest result = factory.createPreauthorizationRequest(paymentWithCartLike);
        SoftAssertions softly = new SoftAssertions();

        //base values
        validateBaseRequestValues(result, RequestType.PREAUTHORIZATION.getType());

        //clearing type
        String clearingType = ClearingType.getClearingTypeByKey(payment.getPaymentMethodInfo().getMethod()).getPayoneCode();
        softly.assertThat(result.getClearingtype()).isEqualTo(clearingType);

        //references
        softly.assertThat(result.getReference()).isEqualTo(paymentWithCartLike.getReference());
        softly.assertThat(result.getCustomerid()).isEqualTo(customer.getCustomerNumber());
        softly.assertThat(result.getLanguage()).isEqualTo(payment.getCustom().getFieldAsString(LANGUAGE_CODE_FIELD));

        //monetary
        softly.assertThat(result.getAmount()).isEqualTo(convertMinorPart().queryFrom(payment.getAmountPlanned()).intValue());
        softly.assertThat(result.getCurrency()).isEqualTo(payment.getAmountPlanned().getCurrency().getCurrencyCode());

        //urls
        softly.assertThat(result.getSuccessurl()).isEqualTo("www.test.de/success");
        softly.assertThat(result.getErrorurl()).isEqualTo("www.test.de/error");
        softly.assertThat(result.getBackurl()).isEqualTo("www.test.de/cancel");

        //3d secure
        softly.assertThat(result.getEcommercemode()).isEqualTo(payment.getCustom().getFieldAsBoolean(CustomFieldKeys.FORCE_3DSECURE_FIELD));

        //billing address data
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

        //shipping address data
        Address shippingAddress = order.getShippingAddress();
        softly.assertThat(result.getShipping_firstname()).isEqualTo(shippingAddress.getFirstName());
        softly.assertThat(result.getShipping_lastname()).isEqualTo(shippingAddress.getLastName());
        softly.assertThat(result.getShipping_street()).isEqualTo(shippingAddress.getStreetName() + " " + shippingAddress.getStreetNumber());
        softly.assertThat(result.getShipping_city()).isEqualTo(shippingAddress.getCity());
        softly.assertThat(result.getShipping_zip()).isEqualTo(shippingAddress.getPostalCode());
        softly.assertThat(result.getShipping_country()).isEqualTo(shippingAddress.getCountry().toLocale().getCountry());
        softly.assertThat(result.getShipping_state()).isEqualTo(shippingAddress.getState());
        softly.assertThat(result.getShipping_company()).isEqualTo(shippingAddress.getCompany() + " " + shippingAddress.getDepartment());

        softly.assertAll();
    }

    @Test
    public void createFullAuthorizationRequestFromValidPayment() throws Exception {
        Payment payment = payments.dummyPaymentOneAuthPending20EuroCC();
        Order order = payments.dummyOrderMapToPayoneRequest();
        Customer customer = payment.getCustomer().getObj();
        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, order);
        CreditCardPayoneRequest result = factory.createAuthorizationRequest(paymentWithCartLike);
        SoftAssertions softly = new SoftAssertions();

        //base values
        softly.assertThat(result.getRequest()).isEqualTo(RequestType.AUTHORIZATION.getType());
        softly.assertThat(result.getAid()).isEqualTo(payoneConfig.getSubAccountId());
        softly.assertThat(result.getMid()).isEqualTo(payoneConfig.getMerchantId());
        softly.assertThat(result.getPortalid()).isEqualTo(payoneConfig.getPortalId());
        softly.assertThat(result.getKey()).isEqualTo(payoneConfig.getKeyAsHash());
        softly.assertThat(result.getMode()).isEqualTo(payoneConfig.getMode());
        softly.assertThat(result.getApiVersion()).isEqualTo(payoneConfig.getApiVersion());
        softly.assertThat(result.getEncoding()).isEqualTo(payoneConfig.getEncoding());
        softly.assertThat(result.getSolutionName()).isEqualTo(payoneConfig.getSolutionName());
        softly.assertThat(result.getSolutionVersion()).isEqualTo(payoneConfig.getSolutionVersion());
        softly.assertThat(result.getIntegratorName()).isEqualTo(payoneConfig.getIntegratorName());
        softly.assertThat(result.getIntegratorVersion()).isEqualTo(payoneConfig.getIntegratorVersion());

        //clearing type
        String clearingType = ClearingType.getClearingTypeByKey(payment.getPaymentMethodInfo().getMethod()).getPayoneCode();
        softly.assertThat(result.getClearingtype()).isEqualTo(clearingType);

        //references
        softly.assertThat(result.getReference()).isEqualTo(paymentWithCartLike.getReference());
        softly.assertThat(result.getCustomerid()).isEqualTo(customer.getCustomerNumber());
        softly.assertThat(result.getLanguage()).isEqualTo(payment.getCustom().getFieldAsString(LANGUAGE_CODE_FIELD));

        //urls
        softly.assertThat(result.getSuccessurl()).isEqualTo("www.test.de/success");
        softly.assertThat(result.getErrorurl()).isEqualTo("www.test.de/error");
        softly.assertThat(result.getBackurl()).isEqualTo("www.test.de/cancel");

        //billing address data
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

        //shipping address data
        Address shippingAddress = order.getShippingAddress();
        softly.assertThat(result.getShipping_firstname()).isEqualTo(shippingAddress.getFirstName());
        softly.assertThat(result.getShipping_lastname()).isEqualTo(shippingAddress.getLastName());
        softly.assertThat(result.getShipping_street()).isEqualTo(shippingAddress.getStreetName() + " " + shippingAddress.getStreetNumber());
        softly.assertThat(result.getShipping_city()).isEqualTo(shippingAddress.getCity());
        softly.assertThat(result.getShipping_zip()).isEqualTo(shippingAddress.getPostalCode());
        softly.assertThat(result.getShipping_country()).isEqualTo(shippingAddress.getCountry().toLocale().getCountry());
        softly.assertThat(result.getShipping_state()).isEqualTo(shippingAddress.getState());
        softly.assertThat(result.getShipping_company()).isEqualTo(shippingAddress.getCompany() + " " + shippingAddress.getDepartment());
        // but then below the order of the items' properties should be also changed
        softly.assertThat(result.getIt()).containsExactly(goods.toString());
        softly.assertThat(result.getId()).containsExactly("MATE-1");
        softly.assertThat(result.getPr()).containsExactly(1190L);
        softly.assertThat(result.getNo()).containsExactly(1L);
        softly.assertThat(result.getDe()).containsExactly("Some Product");
        softly.assertThat(result.getVa()).containsExactly(19);

        softly.assertAll();
    }

    @Test
    public void createFullCaptureRequestFromValidPayment() throws Exception {
        Payment payment = payments.dummyPaymentOneChargePending20Euro();
        Order order = payments.dummyOrderMapToPayoneRequest();
        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, order);
        CreditCardCaptureRequest result = factory.createCaptureRequest(paymentWithCartLike, 0);
        SoftAssertions softly = new SoftAssertions();

        //base values
        softly.assertThat(result.getRequest()).isEqualTo(RequestType.CAPTURE.getType());
        softly.assertThat(result.getMid()).isEqualTo(payoneConfig.getMerchantId());
        softly.assertThat(result.getPortalid()).isEqualTo(payoneConfig.getPortalId());
        softly.assertThat(result.getKey()).isEqualTo(payoneConfig.getKeyAsHash());
        softly.assertThat(result.getMode()).isEqualTo(payoneConfig.getMode());
        softly.assertThat(result.getApiVersion()).isEqualTo(payoneConfig.getApiVersion());
        softly.assertThat(result.getEncoding()).isEqualTo(payoneConfig.getEncoding());
        softly.assertThat(result.getSolutionName()).isEqualTo(payoneConfig.getSolutionName());
        softly.assertThat(result.getSolutionVersion()).isEqualTo(payoneConfig.getSolutionVersion());
        softly.assertThat(result.getIntegratorName()).isEqualTo(payoneConfig.getIntegratorName());
        softly.assertThat(result.getIntegratorVersion()).isEqualTo(payoneConfig.getIntegratorVersion());

        //monetary
        softly.assertThat(result.getAmount()).isEqualTo(convertMinorPart().queryFrom(payment.getAmountPlanned()).intValue());
        softly.assertThat(result.getCurrency()).isEqualTo(payment.getAmountPlanned().getCurrency().getCurrencyCode());

        softly.assertThat(result.getTxid()).isEqualTo(payment.getInterfaceId());
        softly.assertThat(result.getSequencenumber()).isEqualTo(Integer.valueOf(payment.getTransactions().get(0).getInteractionId()));

        softly.assertAll();
    }

}