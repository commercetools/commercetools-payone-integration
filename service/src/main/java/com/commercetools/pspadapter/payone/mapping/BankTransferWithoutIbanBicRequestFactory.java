package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.banktransfer.BankTransferRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.commercetools.util.function.TriFunction;
import com.google.common.base.Preconditions;
import io.sphere.sdk.payments.Payment;

import javax.annotation.Nonnull;

import static com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType.AUTHORIZATION;
import static com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType.PREAUTHORIZATION;

/**
 * Created by mht on 15.08.16.
 */
public class BankTransferWithoutIbanBicRequestFactory extends PayoneRequestFactory {
    public BankTransferWithoutIbanBicRequestFactory(@Nonnull final TenantConfig tenantConfig) {
        super(tenantConfig);
    }

    @Override
    @Nonnull
    public BankTransferRequest createPreauthorizationRequest(@Nonnull final PaymentWithCartLike paymentWithCartLike) {
        return createRequestInternal(PREAUTHORIZATION, paymentWithCartLike, BankTransferRequest::new);
    }

    @Override
    @Nonnull
    public BankTransferRequest createAuthorizationRequest(@Nonnull final PaymentWithCartLike paymentWithCartLike) {
        return createRequestInternal(AUTHORIZATION, paymentWithCartLike, BankTransferRequest::new);
    }

    @Nonnull
    public BankTransferRequest createRequestInternal(@Nonnull final RequestType requestType,
                                                     @Nonnull final PaymentWithCartLike paymentWithCartLike,
                                                     @Nonnull final TriFunction<RequestType, PayoneConfig, String, BankTransferRequest> requestConstructor) {

        final Payment ctPayment = paymentWithCartLike.getPayment();

        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        final String clearingSubType = ClearingType.getClearingTypeByKey(ctPayment.getPaymentMethodInfo().getMethod()).getSubType();
        final BankTransferRequest request = requestConstructor.apply(requestType, getPayoneConfig(), clearingSubType);

        mapFormPaymentWithCartLike(request, paymentWithCartLike);

        //Despite declared as optional in PayOne Server API documentation. the Bankcountry is required
        final String bankCountry = ctPayment.getCustom().getFieldAsString(CustomFieldKeys.BANK_COUNTRY);
        if (bankCountry == null) {
            request.setBankcountry(request.getCountry());
        } else {
            request.setBankcountry(bankCountry);
        }
        return request;
    }
}
