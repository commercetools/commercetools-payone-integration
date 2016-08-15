package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.banktransfer.BankTransferAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.util.BlowfishUtil;
import com.google.common.base.Preconditions;
import io.sphere.sdk.payments.Payment;
import org.javamoney.moneta.function.MonetaryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author fhaertig
 * @since 22.01.16
 * For all Payment methods that require IBAN and BIC
 */
public class BankTransferRequestFactory extends PayoneRequestFactory {

    private static final Logger LOG = LoggerFactory.getLogger(BankTransferRequestFactory.class);

    private final ServiceConfig serviceConfig;

    public BankTransferRequestFactory(final PayoneConfig payoneConfig, final ServiceConfig serviceConfig) {
        super(payoneConfig);
        this.serviceConfig = serviceConfig;
    }

    @Override
    public BankTransferAuthorizationRequest createAuthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {

        final Payment ctPayment = paymentWithCartLike.getPayment();

        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        final String clearingSubType = ClearingType.getClearingTypeByKey(ctPayment.getPaymentMethodInfo().getMethod()).getSubType();
        BankTransferAuthorizationRequest request = new BankTransferAuthorizationRequest(getPayoneConfig(), clearingSubType);

        final String plainIban;
        final String plainBic;
        if (!serviceConfig.getSecureKey().isEmpty()) {
            plainIban = BlowfishUtil.decryptHexToString(serviceConfig.getSecureKey(), ctPayment.getCustom().getFieldAsString(CustomFieldKeys.IBAN_FIELD));
            plainBic = BlowfishUtil.decryptHexToString(serviceConfig.getSecureKey(), ctPayment.getCustom().getFieldAsString(CustomFieldKeys.BIC_FIELD));
        } else {
            plainIban = ctPayment.getCustom().getFieldAsString(CustomFieldKeys.IBAN_FIELD);
            plainBic = ctPayment.getCustom().getFieldAsString(CustomFieldKeys.BIC_FIELD);
        }

        request.setIban(plainIban);
        request.setBic(plainBic);

        request.setReference(paymentWithCartLike.getReference());

        Optional.ofNullable(ctPayment.getAmountPlanned())
                .ifPresent(amount -> {
                    request.setCurrency(amount.getCurrency().getCurrencyCode());
                    request.setAmount(MonetaryUtil
                            .minorUnits()
                            .queryFrom(amount)
                            .intValue());
                });

        mapFormPaymentWithCartLike(request, paymentWithCartLike, LOG);

        return request;
    }
}
