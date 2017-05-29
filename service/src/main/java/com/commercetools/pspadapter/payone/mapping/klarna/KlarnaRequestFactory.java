package com.commercetools.pspadapter.payone.mapping.klarna;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.BaseKlarnaRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaPreauthorizationRequest;
import com.commercetools.pspadapter.payone.mapping.PayoneRequestFactory;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.google.common.base.Preconditions;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.types.CustomFields;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.BIRTHDAY;
import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.IP;

public class KlarnaRequestFactory extends PayoneRequestFactory {

    public KlarnaRequestFactory(@Nonnull final TenantConfig tenantConfig) {
        super(tenantConfig);
    }

    @Override
    public KlarnaPreauthorizationRequest createPreauthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {
        return createRequestInternal(paymentWithCartLike, KlarnaPreauthorizationRequest::new);
    }

    @Override
    public KlarnaAuthorizationRequest createAuthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {
        return createRequestInternal(paymentWithCartLike, KlarnaAuthorizationRequest::new);
    }

    protected <BKR extends BaseKlarnaRequest> BKR createRequestInternal(final PaymentWithCartLike paymentWithCartLike,
                                                                        final TriFunction<PayoneConfig, String, PaymentWithCartLike, BKR> requestConstructor) {
        final Payment ctPayment = paymentWithCartLike.getPayment();
        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        final String clearingSubType = ClearingType.getClearingTypeByKey(ctPayment.getPaymentMethodInfo().getMethod()).getSubType();

        final BKR request = requestConstructor.apply(getPayoneConfig(), clearingSubType, paymentWithCartLike);

        mapFormPaymentWithCartLike(request, paymentWithCartLike);
        mapKlarnaMandatoryFields(request, paymentWithCartLike);

        //the line items, discounts and shipment cost are counted in the Klarna request constructor

        return request;
    }

    /**
     * Map {@code ip} and {@code birthday} from custom fields, if specified. The rest fields expected to be mapped
     * in previous default mapping methods.
     * <p>
     *     <b>Note:</b> birthday from custom fields (if exist) is trimmed to contain only digits, so all of
     *     "1956-07-12", "19560712", 1956/07/12" are valid dates and treated the same way.
     * </p>
     *
     * @param request             request to which write the values (recipient)
     * @param paymentWithCartLike {@link PaymentWithCartLike} from which read the values
     * @return same {@code request} instance.
     */
    protected static AuthorizationRequest mapKlarnaMandatoryFields(@Nonnull AuthorizationRequest request,
                                                                   @Nonnull PaymentWithCartLike paymentWithCartLike) {
        // fistsname, lastname, street, zip, city, country, email, telephonenumber - must be mapped in mapBillingAddressToRequest
        // gender - in mapCustomerToRequest
        // language, amount, currency in mapFormPaymentWithCartLike
        // financingtype - in the constructor

        Optional.of(paymentWithCartLike.getPayment())
                .map(Payment::getCustom)
                .ifPresent(customFields -> mapKlarnaCustomFields(request, customFields));

        return request;
    }

    private static void mapKlarnaCustomFields(@Nonnull AuthorizationRequest request, @Nonnull CustomFields customFields) {
        mapCustomFieldIfSignificant(customFields.getFieldAsString(IP), request::setIp);

        mapCustomFieldIfSignificant(customFields.getFieldAsString(BIRTHDAY), request::setBirthday,
                // Payone requires format yyyyMMdd, but CTP contains dashes "-" in between
                ctpBirthdayString -> ctpBirthdayString.replaceAll("[^\\d.]", ""));
    }

    private static <T> void mapCustomFieldIfSignificant(@Nullable T fieldValue, @Nonnull Consumer<T> fieldConsumer) {
        mapCustomFieldIfSignificant(fieldValue, fieldConsumer, i -> i);
    }

    private static <V, R> void mapCustomFieldIfSignificant(@Nullable V fieldValue, @Nonnull Consumer<R> fieldConsumer,
                                                           @Nonnull Function<V, R> fieldProcessor) {
        if (fieldValue != null) {
            fieldConsumer.accept(fieldProcessor.apply(fieldValue));
        }
    }

    @FunctionalInterface
    private interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}
