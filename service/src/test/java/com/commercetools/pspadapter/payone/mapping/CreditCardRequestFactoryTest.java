package com.commercetools.pspadapter.payone.mapping;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import com.commercetools.pspadapter.payone.PaymentTestHelper;
import com.commercetools.pspadapter.payone.PayoneConfig;
import com.commercetools.pspadapter.payone.ServiceConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CCPreauthorizationRequest;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.payments.Payment;
import org.assertj.core.api.Assertions;
import org.javamoney.moneta.function.MonetaryUtil;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * @author fhaertig
 * @date 14.12.15
 */
public class CreditCardRequestFactoryTest extends PaymentTestHelper {

    private PayoneConfig config;
    private CreditCardRequestFactory factory;

    @Before
    public void setUp() throws MalformedURLException {
        config = new ServiceConfig().getPayoneConfig();
        factory = new CreditCardRequestFactory(config);
    }

    @Test
    public void throwExceptionWhenPaymentIncomplete() throws Exception {
        Payment payment = dummyPaymentNoCustomFields();
        Order order = dummyOrderMapToPayoneRequest();


        final Throwable throwable = catchThrowable(() -> factory.createPreauthorizationRequest(new PaymentWithCartLike(payment, order)));

        Assertions.assertThat(throwable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing custom fields on payment!");
    }

    @Test
    public void createFullPreauthorizationRequestFromValidPayment() throws Exception {

        Payment payment = dummyPaymentMapToPayoneRequest();
        Order order = dummyOrderMapToPayoneRequest();
        PayoneConfig config = new ServiceConfig().getPayoneConfig();
        CCPreauthorizationRequest result = factory.createPreauthorizationRequest(new PaymentWithCartLike(payment, order));

        assertThat(result.getRequest()).isEqualTo(RequestType.PREAUTHORIZATION.getType());
        assertThat(result.getAid()).isEqualTo(config.getSubAccountId());
        assertThat(result.getMid()).isEqualTo(config.getMerchantId());
        assertThat(result.getPortalid()).isEqualTo(config.getPortalId());
        //assertThat(result.getKeyAsMD5Hash()).isEqualTo(config.getKeyAsMD5Hash());
        assertThat(result.getMode()).isEqualTo(config.getMode());
        assertThat(result.getApi_version()).isEqualTo(config.getApiVersion());

        String clearingType = ClearingType.getClearingTypeByKey(payment.getPaymentMethodInfo().getMethod()).getPayoneCode();
        assertThat(result.getClearingtype()).isEqualTo(clearingType);

        assertThat(result.getAmount()).isEqualTo(MonetaryUtil.minorUnits().queryFrom(payment.getAmountPlanned()).intValue());
        assertThat(result.getCurrency()).isEqualTo(payment.getAmountPlanned().getCurrency().getCurrencyCode());

        assertThat(result.getEcommercemode()).isEqualTo(payment.getCustom().getFieldAsBoolean(CustomFieldKeys.FORCE3DSECURE_KEY));

        Address billingAddress = order.getBillingAddress();
        assertThat(result.getTitle()).isEqualTo(billingAddress.getTitle());
        assertThat(result.getSalutation()).isEqualTo(billingAddress.getSalutation());
        assertThat(result.getFirstname()).isEqualTo(billingAddress.getFirstName());
        assertThat(result.getLastname()).isEqualTo(billingAddress.getLastName());
        assertThat(result.getBirthday()).isEqualTo(payment.getCustomer().getObj().getDateOfBirth().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        assertThat(result.getCompany()).isEqualTo(billingAddress.getCompany());
        assertThat(result.getStreet()).isEqualTo(billingAddress.getStreetName() + Optional.ofNullable(billingAddress.getStreetNumber()));
        assertThat(result.getAddressaddition()).isEqualTo(billingAddress.getAdditionalStreetInfo());
        assertThat(result.getCity()).isEqualTo(billingAddress.getCity());
        assertThat(result.getZip()).isEqualTo(order.getBillingAddress().getPostalCode());
        assertThat(result.getCountry()).isEqualTo(order.getBillingAddress().getCountry().toLocale().getCountry());
        assertThat(result.getEmail()).isEqualTo(order.getBillingAddress().getEmail());
        assertThat(result.getTelephonenumber()).isEqualTo(Optional
                .ofNullable(billingAddress.getPhone())
                .orElse(billingAddress.getMobile()));

        assertThat(result.getCustomerid()).isEqualTo(payment.getCustomer().getObj().getCustomerNumber());

        //TODO: need to check also
        //reference
        //gender
        //billingAddress.state


    }




}