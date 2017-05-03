package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.banktransfer.BankTransferAuthorizationRequest;
import com.commercetools.pspadapter.payone.util.BlowfishUtil;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.google.common.base.Preconditions;
import io.sphere.sdk.payments.Payment;
import spark.utils.StringUtils;

import javax.annotation.Nonnull;

/**
 * @author fhaertig
 * @since 22.01.16
 * For all SofortPayment method that with optional IBAN and BIC
 */
public class SofortBankTransferRequestFactory extends BankTransferWithoutIbanBicRequestFactory {

    /**
     * Optional (may be empty string, but not null) key for {@link BlowfishUtil} IBAN/BIC encrypting.
     */
    @Nonnull
    private final String secureKey;

    public SofortBankTransferRequestFactory(@Nonnull final TenantConfig tenantConfig) {
        super(tenantConfig);
        this.secureKey = tenantConfig.getSecureKey() != null ? tenantConfig.getSecureKey() : "";
    }

    @Override
    public BankTransferAuthorizationRequest createAuthorizationRequest(final PaymentWithCartLike paymentWithCartLike) {
        final Payment ctPayment = paymentWithCartLike.getPayment();
        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        BankTransferAuthorizationRequest request = super.createAuthorizationRequest(paymentWithCartLike);

        if (StringUtils.isNotEmpty(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.IBAN_FIELD))) {
            final String plainIban;
            if (!secureKey.isEmpty()) {
                plainIban = BlowfishUtil.decryptHexToString(secureKey, ctPayment.getCustom().getFieldAsString(CustomFieldKeys.IBAN_FIELD));
            } else {
                plainIban = ctPayment.getCustom().getFieldAsString(CustomFieldKeys.IBAN_FIELD);
            }
            request.setIban(plainIban);
        }

        if (StringUtils.isNotEmpty(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.BIC_FIELD))) {
            final String plainBic;
            if (!secureKey.isEmpty()) {
                plainBic = BlowfishUtil.decryptHexToString(secureKey, ctPayment.getCustom().getFieldAsString(CustomFieldKeys.BIC_FIELD));
            } else {
                plainBic = ctPayment.getCustom().getFieldAsString(CustomFieldKeys.BIC_FIELD);
            }
            request.setBic(plainBic);
        }

        return request;
    }
}
