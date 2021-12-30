package com.commercetools.pspadapter.payone.domain.payone.model.common;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaConstants;
import com.commercetools.pspadapter.payone.domain.payone.model.klarna.PricesAggregator;
import io.sphere.sdk.cartdiscounts.DiscountedLineItemPrice;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.carts.CartShippingInfo;
import io.sphere.sdk.carts.CustomLineItem;
import io.sphere.sdk.carts.LineItem;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.shippingmethods.ShippingMethod;
import io.sphere.sdk.utils.MoneyImpl;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaConstants.ID_MAX_LENGTH;
import static com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaItemTypeEnum.goods;
import static com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaItemTypeEnum.shipment;
import static com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaItemTypeEnum.voucher;
import static com.commercetools.pspadapter.payone.mapping.MappingUtil.getPaymentLanguageTagOrFallback;
import static com.commercetools.pspadapter.payone.util.MoneyUtil.getActualPricePerLineItem;
import static com.commercetools.pspadapter.payone.util.MoneyUtil.getTaxRateIfPresentOrZero;
import static com.commercetools.util.ServiceConstants.DEFAULT_LOCALE;
import static io.sphere.sdk.utils.MoneyImpl.centAmountOf;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;

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
public abstract class AuthorizationRequestWithCart extends PayoneRequestWithCart {



    protected AuthorizationRequestWithCart(@Nonnull final PayoneConfig config,
                                           @Nonnull final String requestType,
                                           @Nullable final String financingtype,
                                           @Nonnull final String clearingType,
                                           @Nonnull final PaymentWithCartLike paymentWithCartLike) {
        super(config, requestType,financingtype, clearingType, paymentWithCartLike);


    }




}
