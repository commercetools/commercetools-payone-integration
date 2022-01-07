package com.commercetools.pspadapter.payone.domain.payone.model.common;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Base class for credit card and Klarna <i>authorisation</i> and <i>preauthorisation</i> requests.
 * <p>
 * Opposite to most of other requests, this requests has high level requirements for sold items description,
 * thus we map all the line items, custom items, discounts and shipment costs in
 * {@link #mapPropertiesFromPaymentCart(PaymentWithCartLike)} from the {@link PaymentWithCartLike}.
 * <p>
 * See <i>https://docs.payone.com/display/public/PLATFORM/Special+remarks+-+3-D+Secure</i> and
 * <i>PAYONE_Platform_Klarna_Addon_EN</i>   for more details.
 * The latest implemented API version is from <b>2016-11-30</b>.
 */
public abstract class StartSessionRequestWithCart extends PayoneRequestWithCart {




    public static final String ACTION_KEY = "action";
    public static final String ACTION_VALUE = "start_session";

    public StartSessionRequestWithCart(@Nonnull final PayoneConfig config,
                                       @Nonnull final String requestType,
                                       @Nullable final String financingtype,
                                       @Nonnull final String clearingType,
                                       @Nonnull final PaymentWithCartLike paymentWithCartLike,
                                       @Nullable Map<String, String> payDataMap) {

        super(config, requestType, financingtype, clearingType, paymentWithCartLike);
        appendPaymentData(ACTION_KEY, ACTION_VALUE);
        if (payDataMap != null) {
            super.getPayData().putAll(payDataMap);
        }
    }
}
