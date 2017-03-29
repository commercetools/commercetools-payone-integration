package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.banktransfer.BankTransferAuthorizationRequest;
import com.commercetools.pspadapter.payone.util.BlowfishUtil;
import com.google.common.base.Preconditions;
import io.sphere.sdk.payments.Payment;
import spark.utils.StringUtils;

/**
 * @author fhaertig
 * @since 22.01.16
 * For all SofortPayment method that with optional IBAN and BIC
 */
public class SofortBankTransferRequestFactory extends BankTransferWithoutIbanBicRequestFactory {

    private final ServiceConfig serviceConfig;

    public SofortBankTransferRequestFactory(final ServiceConfig serviceConfig) {
        super(serviceConfig.getPayoneConfig());
        this.serviceConfig = serviceConfig;
    }

    @Override
    public BankTransferAuthorizationRequest createAuthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {
        final Payment ctPayment = paymentWithCartLike.getPayment();
        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        BankTransferAuthorizationRequest request = super.createAuthorizationRequest(paymentWithCartLike);

        if (StringUtils.isNotEmpty(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.IBAN_FIELD))) {
            final String plainIban;
            if (!serviceConfig.getSecureKey().isEmpty()) {
                plainIban = BlowfishUtil.decryptHexToString(serviceConfig.getSecureKey(), ctPayment.getCustom().getFieldAsString(CustomFieldKeys.IBAN_FIELD));
            } else {
                plainIban = ctPayment.getCustom().getFieldAsString(CustomFieldKeys.IBAN_FIELD);
            }
            request.setIban(plainIban);
        }

        if (StringUtils.isNotEmpty(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.BIC_FIELD))) {
            final String plainBic;
            if (!serviceConfig.getSecureKey().isEmpty()) {
                plainBic = BlowfishUtil.decryptHexToString(serviceConfig.getSecureKey(), ctPayment.getCustom().getFieldAsString(CustomFieldKeys.BIC_FIELD));
            } else {
                plainBic = ctPayment.getCustom().getFieldAsString(CustomFieldKeys.BIC_FIELD);
            }
            request.setBic(plainBic);
        }

        return request;
    }
}
