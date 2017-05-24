package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.banktransfer.BankTransferAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.banktransfer.BankTransferPreathorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.banktransfer.BaseBankTransferAuthorizationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.function.BiFunction;

/**
 * Created by mht on 15.08.16.
 */
public class BankTransferWithoutIbanBicRequestFactory extends PayoneRequestFactory {
    public BankTransferWithoutIbanBicRequestFactory(PayoneConfig payoneConfig) {
        super(payoneConfig);
    }

    private static final Logger LOG = LoggerFactory.getLogger(BankTransferWithoutIbanBicRequestFactory.class);

    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public BaseBankTransferAuthorizationRequest createPreauthorizationRequest(@Nonnull PaymentWithCartLike paymentWithCartLike) {
        return createBankTransferRequest(paymentWithCartLike, BankTransferPreathorizationRequest::new);
    }

    @Override
    public BaseBankTransferAuthorizationRequest createAuthorizationRequest(@Nonnull final PaymentWithCartLike paymentWithCartLike) {
        return createBankTransferRequest(paymentWithCartLike, BankTransferAuthorizationRequest::new);
    }

    protected BaseBankTransferAuthorizationRequest createBankTransferRequest(@Nonnull final PaymentWithCartLike paymentWithCartLike,
                                                                             @Nonnull final BiFunction<PayoneConfig, String, BaseBankTransferAuthorizationRequest> requestGenerator) {

        BaseBankTransferAuthorizationRequest request = createBasicAuthorizationRequest(paymentWithCartLike, requestGenerator, getLogger());

        //Despite declared as optional in PayOne Server API documentation. the Bankcountry is required
        request.setBankcountry(request.getCountry());
        return request;
    }
}
