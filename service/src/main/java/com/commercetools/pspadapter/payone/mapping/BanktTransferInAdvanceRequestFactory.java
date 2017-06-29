package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.CaptureRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.paymentinadvance.BankTransferInAdvanceCaptureRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.paymentinadvance.BankTransferInAdvancePreautorizationRequest;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.google.common.base.Preconditions;
import io.sphere.sdk.payments.Payment;
import org.javamoney.moneta.function.MonetaryUtil;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 *
 * @author mht@dotsource.de
 *
 */
public class BanktTransferInAdvanceRequestFactory extends PayoneRequestFactory {

    public BanktTransferInAdvanceRequestFactory(@Nonnull final TenantConfig tenantConfig) {
        super(tenantConfig);
    }

    @Override
    public BankTransferInAdvancePreautorizationRequest createPreauthorizationRequest(PaymentWithCartLike paymentWithCartLike) {
        final Payment ctPayment = paymentWithCartLike.getPayment();

        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        final String clearingSubType = ClearingType.getClearingTypeByKey(ctPayment.getPaymentMethodInfo().getMethod()).getSubType();
        BankTransferInAdvancePreautorizationRequest request = new BankTransferInAdvancePreautorizationRequest(getPayoneConfig(), clearingSubType);

        request.setReference(paymentWithCartLike.getReference());

        Optional.ofNullable(ctPayment.getAmountPlanned())
                .ifPresent(m -> request.setAmount(MonetaryUtil
                                                    .minorUnits()
                                                    .queryFrom(ctPayment.getAmountPlanned())
                                                    .intValue()));
        request.setCurrency(ctPayment.getAmountPlanned().getCurrency().getCurrencyCode());

        mapFormPaymentWithCartLike(request, paymentWithCartLike);

        return request;
    }

    @Override
    public CaptureRequest createCaptureRequest(final PaymentWithCartLike paymentWithCartLike, final int sequenceNumber) {

        final Payment ctPayment = paymentWithCartLike.getPayment();

        CaptureRequest request = new BankTransferInAdvanceCaptureRequest(getPayoneConfig());

        request.setTxid(ctPayment.getInterfaceId());

        request.setSequencenumber(sequenceNumber);
        Optional.ofNullable(ctPayment.getAmountPaid())
                .ifPresent(amount -> {
                    request.setCurrency(amount.getCurrency().getCurrencyCode());
                    request.setAmount(MonetaryUtil
                            .minorUnits()
                            .queryFrom(amount)
                            .intValue());
                });

        return request;
    }
}
