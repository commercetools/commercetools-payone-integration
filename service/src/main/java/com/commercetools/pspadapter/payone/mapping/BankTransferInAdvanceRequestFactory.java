package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.CaptureRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import com.commercetools.pspadapter.payone.domain.payone.model.paymentinadvance.BankTransferInAdvanceCaptureRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.paymentinadvance.BankTransferInAdvanceRequest;
import com.commercetools.pspadapter.tenant.TenantConfig;
import io.sphere.sdk.payments.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.BiFunction;

import static com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod.BANK_TRANSFER_ADVANCE;
import static org.javamoney.moneta.function.MonetaryQueries.convertMinorPart;

/**
 * @author mht@dotsource.de
 */
public class BankTransferInAdvanceRequestFactory extends PayoneRequestFactory {

    private Logger LOG = LoggerFactory.getLogger(BankTransferInAdvanceRequestFactory.class);

    public BankTransferInAdvanceRequestFactory(@Nonnull final TenantConfig tenantConfig) {
        super(tenantConfig);
    }

    @Nonnull
    @Override
    public BankTransferInAdvanceRequest createPreauthorizationRequest(@Nonnull PaymentWithCartLike paymentWithCartLike) {
        return createRequestInternal(RequestType.PREAUTHORIZATION, paymentWithCartLike, BankTransferInAdvanceRequest::new);
    }

    /**
     * <b>NOTE:</b> this is a potentially dangerous transaction type for this payment method. See more in
     * {@link com.commercetools.pspadapter.payone.transaction.paymentinadvance.BankTransferInAdvanceChargeTransactionExecutor BankTransferInAdvanceChargeTransactionExecutor}
     * @see com.commercetools.pspadapter.payone.transaction.paymentinadvance.BankTransferInAdvanceChargeTransactionExecutor
     */
    @Nonnull
    @Override
    public BankTransferInAdvanceRequest createAuthorizationRequest(@Nonnull PaymentWithCartLike paymentWithCartLike) {
        LOG.warn("Unsupported transaction type \"{}\" for payment method \"{}\".\n" +
                        "\t\tNote: this payment/transaction type officially isn't supported by Payone " +
                        "and thus the behavior of such transaction handling is undefined.\n" +
                        "Either change the transaction type or update the service if the Payone API has changed.",
                RequestType.AUTHORIZATION, BANK_TRANSFER_ADVANCE.getKey());
        return createRequestInternal(RequestType.AUTHORIZATION, paymentWithCartLike, BankTransferInAdvanceRequest::new);
    }

    @Nonnull
    public BankTransferInAdvanceRequest createRequestInternal(
            @Nonnull final RequestType requestType,
            @Nonnull final PaymentWithCartLike paymentWithCartLike,
            @Nonnull final BiFunction<RequestType, PayoneConfig, BankTransferInAdvanceRequest> requestConstructor) {

        final Payment ctPayment = paymentWithCartLike.getPayment();
        if(ctPayment.getCustom() == null) {
            throw new IllegalArgumentException("Missing custom fields on payment!");
        }
        BankTransferInAdvanceRequest request = requestConstructor.apply(requestType, getPayoneConfig());
        mapFormPaymentWithCartLike(request, paymentWithCartLike, false);
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
