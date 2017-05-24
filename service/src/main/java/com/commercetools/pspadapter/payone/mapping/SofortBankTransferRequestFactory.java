package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.banktransfer.BaseBankTransferAuthorizationRequest;
import com.commercetools.pspadapter.payone.util.BlowfishUtil;
import com.google.common.base.Preconditions;
import io.sphere.sdk.payments.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.utils.StringUtils;

import javax.annotation.Nonnull;

/**
 * @author fhaertig
 * @since 22.01.16
 * For all SofortPayment method that with optional IBAN and BIC
 */
public class SofortBankTransferRequestFactory extends BankTransferWithoutIbanBicRequestFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SofortBankTransferRequestFactory.class);

    private final ServiceConfig serviceConfig;

    public SofortBankTransferRequestFactory(final PayoneConfig payoneConfig, final ServiceConfig serviceConfig) {
        super(payoneConfig);
        this.serviceConfig = serviceConfig;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public BaseBankTransferAuthorizationRequest createPreauthorizationRequest(@Nonnull PaymentWithCartLike paymentWithCartLike) {
        return wrapSofortRequest(super.createPreauthorizationRequest(paymentWithCartLike), paymentWithCartLike);
    }

    @Override
    public BaseBankTransferAuthorizationRequest createAuthorizationRequest(@Nonnull final PaymentWithCartLike paymentWithCartLike) {
        return wrapSofortRequest(super.createAuthorizationRequest(paymentWithCartLike), paymentWithCartLike);
    }

    /**
     * Add sofort specific values to the Basic bank request (if the values exist):
     * <ul>
     *     <li>iban</li>
     *     <li>bic</li>
     * </ul>
     * @param request request to which add the values
     * @param paymentWithCartLike payment from which map the values
     * @return the same {@code request} instance with added optional values.
     */
    private BaseBankTransferAuthorizationRequest wrapSofortRequest(@Nonnull BaseBankTransferAuthorizationRequest request,
                                                                   @Nonnull final PaymentWithCartLike paymentWithCartLike) {
        final Payment ctPayment = paymentWithCartLike.getPayment();
        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

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
