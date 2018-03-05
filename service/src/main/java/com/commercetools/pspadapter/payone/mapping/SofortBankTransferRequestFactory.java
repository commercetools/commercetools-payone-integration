package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.banktransfer.BankTransferRequest;
import com.commercetools.pspadapter.payone.util.BlowfishUtil;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.google.common.base.Preconditions;
import io.sphere.sdk.payments.Payment;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.BIC_FIELD;
import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.IBAN_FIELD;
import static java.util.Optional.ofNullable;

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
    @Nonnull
    public BankTransferRequest createAuthorizationRequest(@Nonnull final PaymentWithCartLike paymentWithCartLike) {
        return wrapSofortBankTransferRequest(super.createAuthorizationRequest(paymentWithCartLike), paymentWithCartLike);
    }

    @Override
    @Nonnull
    public BankTransferRequest createPreauthorizationRequest(@Nonnull final PaymentWithCartLike paymentWithCartLike) {
        return wrapSofortBankTransferRequest(super.createPreauthorizationRequest(paymentWithCartLike), paymentWithCartLike);
    }

    /**
     * Add Sofort banking specific fields (like IBAN/BIC) to default {@link BankTransferRequest}, if they exist in
     * {@code paymentWithCartLike} custom fields.
     *
     * @param bankTransferRequest basic {@link BankTransferRequest} to update.
     * @param paymentWithCartLike payment holder from which read Sofort banking specific custom fields.
     * @return {@link BankTransferRequest} instance with updated fields from {@code paymentWithCartLike}
     */
    @Nonnull
    protected BankTransferRequest wrapSofortBankTransferRequest(@Nonnull final BankTransferRequest bankTransferRequest,
                                                                @Nonnull final PaymentWithCartLike paymentWithCartLike) {
        final Payment ctPayment = paymentWithCartLike.getPayment();
        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        setBankField(ctPayment, IBAN_FIELD, bankTransferRequest::setIban);
        setBankField(ctPayment, BIC_FIELD, bankTransferRequest::setBic);

        return bankTransferRequest;
    }

    /**
     * Decrypt (if {@link #secureKey} is provided and set corresponding bank field (iban or bic) if it exists in the
     * {@code ctPayment}'s custom field.
     *
     * @param ctPayment         Payment which might have iban/bic values in the custom fields.
     * @param fieldKey          name of field (see {@link CustomFieldKeys#IBAN_FIELD} and {@link CustomFieldKeys#BIC_FIELD}
     * @param bankFieldConsumer {@link Consumer} which sets iban/bic,
     *                          see {@link BankTransferRequest#setIban(String)}
     *                          and {@link BankTransferRequest#setBic(String)}
     */
    private void setBankField(@Nonnull Payment ctPayment,
                              @Nonnull String fieldKey, @Nonnull Consumer<String> bankFieldConsumer) {

        ofNullable(ctPayment.getCustom())
            .map(customFields -> customFields.getFieldAsString(fieldKey))
            .filter(StringUtils::isNotBlank)
            .ifPresent(bankFieldAsString ->
                    bankFieldConsumer.accept(secureKey.isEmpty()
                            ? bankFieldAsString
                            : BlowfishUtil.decryptHexToString(secureKey, bankFieldAsString)));
    }
}
