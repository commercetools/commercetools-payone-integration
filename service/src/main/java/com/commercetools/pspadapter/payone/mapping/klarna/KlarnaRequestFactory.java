package com.commercetools.pspadapter.payone.mapping.klarna;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneRequestWithCart;
import com.commercetools.pspadapter.payone.domain.payone.model.common.StartSessionRequestWithCart;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaPreauthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaStartSessionRequest;
import com.commercetools.pspadapter.payone.mapping.CountryToLanguageMapper;
import com.commercetools.pspadapter.payone.mapping.MappingUtil;
import com.commercetools.pspadapter.payone.mapping.PayoneRequestFactory;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.commercetools.util.function.TriFunction;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.types.CustomFields;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.AUTHORIZATION_TOKEN;
import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.BIRTHDAY_FIELD;
import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.IP_FIELD;
import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.TELEPHONENUMBER_FIELD;
import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.WORK_ORDER_ID_FIELD;
import static com.commercetools.pspadapter.payone.mapping.MappingUtil.getFirstValueFromAddresses;
import static java.util.Arrays.asList;

public class KlarnaRequestFactory extends PayoneRequestFactory {

    public static final String KLARNA_AUTHORIZATION_TOKEN = "authorization_token";
    @Nonnull
    private final CountryToLanguageMapper countryToLanguageMapper;

    public KlarnaRequestFactory(@Nonnull final TenantConfig tenantConfig,
                                @Nonnull final CountryToLanguageMapper countryToLanguageMapper) {
        super(tenantConfig);
        this.countryToLanguageMapper = countryToLanguageMapper;
    }

    @Override
    @Nonnull
    public KlarnaPreauthorizationRequest createPreauthorizationRequest(@Nonnull final PaymentWithCartLike paymentWithCartLike) {
        return createRequestInternal(paymentWithCartLike, KlarnaPreauthorizationRequest::new);
    }

    @Nonnull
    public StartSessionRequestWithCart createStartSessionRequest(@Nonnull final PaymentWithCartLike paymentWithCartLike) {
        return createRequestInternal(paymentWithCartLike, KlarnaStartSessionRequest::new);
    }
    /**
     * @see KlarnaAuthorizationRequest
     */
    @Override
    @Nonnull
    public KlarnaAuthorizationRequest createAuthorizationRequest(@Nonnull final PaymentWithCartLike paymentWithCartLike) {
        return createRequestInternal(paymentWithCartLike, KlarnaAuthorizationRequest::new);
    }

    @Nonnull
    protected <BKR extends PayoneRequestWithCart> BKR createRequestInternal(@Nonnull final PaymentWithCartLike paymentWithCartLike,
                                                                                   @Nonnull final TriFunction<PayoneConfig, String, PaymentWithCartLike, BKR> requestConstructor) {
        final Payment ctPayment = paymentWithCartLike.getPayment();

        if(ctPayment.getCustom() == null) {
            throw new IllegalArgumentException("Missing custom fields on payment!");
        }

        final String clearingSubType = ClearingType.getClearingTypeByKey(ctPayment.getPaymentMethodInfo().getMethod()).getSubType();

        final BKR request = requestConstructor.apply(getPayoneConfig(), clearingSubType, paymentWithCartLike);
        boolean ignoreShippingAddress = true;
        mapFormPaymentWithCartLike(request, paymentWithCartLike, ignoreShippingAddress);
        mapKlarnaMandatoryFields(request, paymentWithCartLike);

        //the line items, discounts and shipment cost are counted in the Klarna request constructor

        return request;
    }
    /**
     * 1. Map {@code ip}, {@code birthday} {@code telephonenumber} from custom fields, if specified.
     * <p>
     * 2. Re-map {@code language} from address country code. Payone/Klarna requires the country and language request
     * arguments match each other. See <i>PAYONE_Platform_Klarna_Addon_EN_2016-11-30</i>
     * documentation for further details.
     * </p>
     * <p>
     * The rest of the request fields expected to be mapped in previous default mapping methods.
     * </p>
     *
     * @param request             request to which write the values (recipient)
     * @param paymentWithCartLike {@link PaymentWithCartLike} from which read the values
     * @return same {@code request} instance.
     */
    protected <BKR extends PayoneRequestWithCart>  void mapKlarnaMandatoryFields(@Nonnull BKR request,
                                                     @Nonnull PaymentWithCartLike paymentWithCartLike) {
         // authorisationToken  - must be mapped in paydata
        // fistsname, lastname, street, zip, city, country, email - must be mapped in mapBillingAddressToRequest
        // gender - in mapCustomerToRequest
        // language, amount, currency in mapFormPaymentWithCartLike
        // financingtype - in the constructor

        Optional.of(paymentWithCartLike.getPayment())
                .map(Payment::getCustom)
                .ifPresent(customFields -> mapKlarnaCustomFields(request, customFields));
        // Get language from the address, if it is not already set to the request
        if(StringUtils.isEmpty(request.getLanguage())) {
            mapLanguageFromCountry(request, paymentWithCartLike.getCartLike());
        }
    }

    private static void mapKlarnaCustomFields(@Nonnull PayoneRequestWithCart request, @Nonnull CustomFields customFields) {

        // Add authorisation token to paydata
        mapCustomFieldIfSignificant(customFields.getFieldAsString(AUTHORIZATION_TOKEN),
                token -> request.appendPaymentData(KLARNA_AUTHORIZATION_TOKEN, token));

        mapCustomFieldIfSignificant(customFields.getFieldAsString(IP_FIELD), request::setIp);
        mapCustomFieldIfSignificant(customFields.getFieldAsString(WORK_ORDER_ID_FIELD), request::setWorkorderid);
        mapCustomFieldIfSignificant(customFields.getFieldAsDate(BIRTHDAY_FIELD), request::setBirthday,
                MappingUtil::dateToBirthdayString);

        // override telephone number from billing address, if custom field is specified
        mapCustomFieldIfSignificant(customFields.getFieldAsString(TELEPHONENUMBER_FIELD), request::setTelephonenumber);
    }

    /**
     * Set {@code fieldValue} to {@code fieldConsumer} if it is not <b>null</b>.
     *
     * @param fieldValue    Value to verify and set.
     * @param fieldConsumer {@link Consumer} to apply for the value, if it is not <b>null</b>.
     * @param <T>           type of {@code fieldValue}
     */
    private static <T> void mapCustomFieldIfSignificant(@Nullable T fieldValue, @Nonnull Consumer<T> fieldConsumer) {
        mapCustomFieldIfSignificant(fieldValue, fieldConsumer, i -> i);
    }

    /**
     * If {@code fieldValue} is not <b>null</b> - convert it by {@code fieldProcessor} (like convert date to string)
     * and set to {@code fieldConsumer}.
     *
     * @param fieldValue     value to validate and set.
     * @param fieldConsumer  {@link Consumer} which accepts converted value.
     * @param fieldProcessor {@link Function} which converts {@code fieldValue} from {@code V} to {@code R} type.
     * @param <V>            type of fieldValue
     * @param <R>            type of value to set to the {@code fieldConsumer}
     */
    private static <V, R> void mapCustomFieldIfSignificant(@Nullable V fieldValue, @Nonnull Consumer<R> fieldConsumer,
                                                           @Nonnull Function<V, R> fieldProcessor) {
        if (fieldValue != null) {
            fieldConsumer.accept(fieldProcessor.apply(fieldValue));
        }
    }

    /**
     * Map country code from billing/shipping address to respective language if it is set and the mapping exists.
     * If country not set in the addresses, or {@link #countryToLanguageMapper} doesn't have respective mapping -
     * don't alter {@link PayoneRequest#setLanguage(String)}
     * @param request {@link PayoneRequest} where update the language
     * @param cartLike {@link CartLike} from which to read the addresses/language settings.
     */
    protected void mapLanguageFromCountry(@Nonnull PayoneRequest request, @Nonnull CartLike<?> cartLike) {
        countryToLanguageMapper.mapCountryToLanguage(request.getCountry()) // try to take country-locale from the request
                .map(Optional::of)
                .orElseGet(() -> // otherwise try to get country-locale from the cart
                        getFirstValueFromAddresses(asList(cartLike.getBillingAddress(), cartLike.getShippingAddress()),
                                Address::getCountry)
                                .flatMap(countryToLanguageMapper::mapCountryToLanguage))
                .map(Locale::getLanguage)
                .ifPresent(request::setLanguage);
    }
}
