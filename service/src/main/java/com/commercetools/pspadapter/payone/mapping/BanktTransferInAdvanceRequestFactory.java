package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.CaptureRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import com.commercetools.pspadapter.payone.domain.payone.model.paymentinadvance.BankTransferInAdvanceCaptureRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.paymentinadvance.BankTransferInAdvanceRequest;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.google.common.base.Preconditions;
import io.sphere.sdk.payments.Payment;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.javamoney.moneta.function.MonetaryQueries.convertMinorPart;

/**
 * @author mht@dotsource.de
 */
public class BanktTransferInAdvanceRequestFactory extends PayoneRequestFactory {

    public BanktTransferInAdvanceRequestFactory(@Nonnull final TenantConfig tenantConfig) {
        super(tenantConfig);
    }

    @Override
    @Nonnull
    public BankTransferInAdvanceRequest createPreauthorizationRequest(@Nonnull PaymentWithCartLike paymentWithCartLike) {
        return createRequestInternal(RequestType.PREAUTHORIZATION, paymentWithCartLike, BankTransferInAdvanceRequest::new);
    }

    @Nonnull
    @Override
    public BankTransferInAdvanceRequest createAuthorizationRequest(@Nonnull PaymentWithCartLike paymentWithCartLike) {
        return createRequestInternal(RequestType.AUTHORIZATION, paymentWithCartLike, BankTransferInAdvanceRequest::new);
    }

    @Nonnull
    public BankTransferInAdvanceRequest createRequestInternal(
            @Nonnull final RequestType requestType,
            @Nonnull final PaymentWithCartLike paymentWithCartLike,
            @Nonnull final BiFunction<RequestType, PayoneConfig, BankTransferInAdvanceRequest> requestConstructor) {

        final Payment ctPayment = paymentWithCartLike.getPayment();
        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");
        BankTransferInAdvanceRequest request = requestConstructor.apply(requestType, getPayoneConfig());
        mapFormPaymentWithCartLike(request, paymentWithCartLike);
        return request;
    }

    @Override
    public CaptureRequest createCaptureRequest(final PaymentWithCartLike paymentWithCartLike, final int sequenceNumber) {

        final Payment ctPayment = paymentWithCartLike.getPayment();

        CaptureRequest request = new BankTransferInAdvanceCaptureRequest(getPayoneConfig());

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
