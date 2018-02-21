package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CreditCardAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CreditCardCaptureRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CreditCardPreauthorizationRequest;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.google.common.base.Preconditions;
import io.sphere.sdk.payments.Payment;

import javax.annotation.Nonnull;
import java.util.Optional;

import static org.javamoney.moneta.function.MonetaryQueries.convertMinorPart;

/**
 * @author fhaertig
 * @since 13.12.15
 */
public class CreditCardRequestFactory extends PayoneRequestFactory {

    public CreditCardRequestFactory(@Nonnull final TenantConfig tenantConfig) {
        super(tenantConfig);
    }

    @Override
    @Nonnull
    public CreditCardPreauthorizationRequest createPreauthorizationRequest(
            final PaymentWithCartLike paymentWithCartLike) {

        final Payment ctPayment = paymentWithCartLike.getPayment();

        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        final String pseudocardpan = ctPayment.getCustom().getFieldAsString(CustomFieldKeys.CARD_DATA_PLACEHOLDER_FIELD);
        CreditCardPreauthorizationRequest request = new CreditCardPreauthorizationRequest(getPayoneConfig(), pseudocardpan);

        mapFormPaymentWithCartLike(request, paymentWithCartLike);

        return request;
    }

    @Override
    @Nonnull
    public CreditCardAuthorizationRequest createAuthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {

        final Payment ctPayment = paymentWithCartLike.getPayment();

        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        final String pseudocardpan = ctPayment.getCustom().getFieldAsString(CustomFieldKeys.CARD_DATA_PLACEHOLDER_FIELD);
        CreditCardAuthorizationRequest request = new CreditCardAuthorizationRequest(getPayoneConfig(), pseudocardpan);

        mapFormPaymentWithCartLike(request, paymentWithCartLike);

        return request;
    }

    @Override
    public CreditCardCaptureRequest createCaptureRequest(final PaymentWithCartLike paymentWithCartLike, final int sequenceNumber) {

        final Payment ctPayment = paymentWithCartLike.getPayment();

        CreditCardCaptureRequest request = new CreditCardCaptureRequest(getPayoneConfig());

        request.setTxid(ctPayment.getInterfaceId());

        request.setSequencenumber(sequenceNumber);
        Optional.ofNullable(ctPayment.getAmountPlanned())
                .ifPresent(amount -> {
                    request.setCurrency(amount.getCurrency().getCurrencyCode());
                    request.setAmount(convertMinorPart()
                            .queryFrom(amount)
                            .intValue());
                });

        return request;
    }
}
