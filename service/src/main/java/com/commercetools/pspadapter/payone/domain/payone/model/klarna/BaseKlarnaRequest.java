package com.commercetools.pspadapter.payone.domain.payone.model.klarna;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import io.sphere.sdk.cartdiscounts.DiscountedLineItemPrice;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.carts.LineItemLike;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.shippingmethods.ShippingMethod;
import io.sphere.sdk.taxcategories.TaxRate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.money.MonetaryAmount;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.commercetools.pspadapter.payone.domain.payone.model.klarna.KlarnaItemTypeEnum.*;
import static com.commercetools.pspadapter.payone.mapping.MappingUtil.getPaymentLanguageTagOrFallback;
import static com.commercetools.util.ServiceConstants.DEFAULT_LOCALE;
import static java.util.Optional.ofNullable;

/**
 * Base class for Klarna <i>authorisation</i> and <i>preauthorisation</i> requests.
 * <p>
 * Opposite to most of other requests, this requests has high level requirements for sold items description,
 * thus we map all the line items, custom items, discounts and shipment costs in
 * {@link #mapKlarnaPropertiesFromPaymentCart(PaymentWithCartLike)} from the {@link PaymentWithCartLike}.
 * <p>
 * See <i>PAYONE_Platform_Klarna_Addon_EN</i> for more details.
 * The latest implemented API version is from <b>2016-11-30</b>.
 */
public abstract class BaseKlarnaRequest extends AuthorizationRequest {

    private String financingtype;

    private List<String> it;

    private List<String> id;

    private List<Integer> pr;

    private List<Long> no;

    private List<String> de;

    private List<Double> va;

    protected BaseKlarnaRequest(final PayoneConfig config, final String requestType,
                                final String financingtype, final PaymentWithCartLike paymentWithCartLike) {
        super(config, requestType, ClearingType.PAYONE_KLV.getPayoneCode());

        this.financingtype = financingtype;

        mapKlarnaPropertiesFromPaymentCart(paymentWithCartLike);
    }

    /**
     * <ul>
     * <li><b>KLV</b> - Klarna Invoicing</li>
     * <li><b>KLS</b> - Klarna Installment</li>
     * </ul>
     *
     * @return Klarna financing type. For now only KLV is implemented, see {@link ClearingType#PAYONE_KLV}
     */
    public String getFinancingtype() {
        return financingtype;
    }

    /**
     * Item types list:<ul>
     * <li><b>goods</b> - Goods</li>
     * <li><b>shipment</b> - Shipping charges</li>
     * <li><b>handling</b> - Handling fee</li>
     * <li><b>voucher</b> - Voucher / discount</li>
     * </ul>
     *
     * @return list of all price item types (line items, shipment, voucher/discount), respective to {@link #getId()}
     */
    public List<String> getIt() {
        return it;
    }

    public void setIt(List<String> it) {
        this.it = it;
    }

    /**
     * Product number
     *
     * @return list of product numbers (article, artikel)
     */
    public List<String> getId() {
        return id;
    }

    public void setId(List<String> id) {
        this.id = id;
    }

    /**
     * Unit price
     * <p>
     * (in smallest currency unit! e.g. cent)
     *
     * @return list of product prices, respective to {@link #getIt()} and {@link #getId()}
     */
    public List<Integer> getPr() {
        return pr;
    }

    public void setPr(List<Integer> pr) {
        this.pr = pr;
    }

    /**
     * @return items quantity, respective to {@link #getId()}
     */
    public List<Long> getNo() {
        return no;
    }

    public void setNo(List<Long> no) {
        this.no = no;
    }

    /**
     * @return list of items description, respective to {@link #getId()}
     */
    public List<String> getDe() {
        return de;
    }

    public void setDe(List<String> de) {
        this.de = de;
    }


    /**
     * VAT rate (%)
     * <p>
     * value < 100 = percent
     *
     * @return list of VAT rates in percent, respective to {@link #getId()}
     */
    public List<Double> getVa() {
        return va;
    }

    public void setVa(List<Double> va) {
        this.va = va;
    }

    /**
     * This function will clean/initialize all the list properties(like {@link #id}, {@link #it} and so on) and
     * populate them from the {@code paymentWithCartLike} (custom)line items, discounts, shipment costs.
     *
     * @param paymentWithCartLike instance from which all the prices are fetched.
     */
    protected void mapKlarnaPropertiesFromPaymentCart(@Nonnull PaymentWithCartLike paymentWithCartLike) {
        final Locale orderLocale = Locale.forLanguageTag(getPaymentLanguageTagOrFallback(paymentWithCartLike));
        final Set<Locale> localesList = Stream.of(orderLocale, DEFAULT_LOCALE).collect(Collectors.toSet());

        final CartLike<?> cartLike = paymentWithCartLike.getCartLike();
        final int itemsCount = cartLike.getLineItems().size()
                + cartLike.getCustomLineItems().size()
                + 1; // cartLike.getShippingInfo();

        this.id = new ArrayList<>(itemsCount);
        this.it = new ArrayList<>(itemsCount);
        this.pr = new ArrayList<>(itemsCount);
        this.no = new ArrayList<>(itemsCount);
        this.de = new ArrayList<>(itemsCount);
        this.va = new ArrayList<>(itemsCount);

        // accumulate all items types discounts to a single value
        AtomicInteger discountAccumulator = new AtomicInteger(0);

        // populate "goods" items
        cartLike.getLineItems().forEach(lineItem -> {
                    it.add(goods.toString());
                    id.add(lineItem.getVariant().getSku());

                    // TODO: the discounts are not separated as dedicated items in the Klarna request,
                    // instead below we calculate total discounts of the cart
                    pr.add(getCentsAmount(lineItem.getPrice().getValue()));

                    no.add(lineItem.getQuantity());

                    de.add(localizeOrFallback(lineItem.getName(), localesList, "item"));
                    va.add(getTaxRateIfPresentOrZero(lineItem.getTaxRate()));

                    discountAccumulator.addAndGet(getTotalDiscountedPricePerItem(lineItem));
                }
        );

        cartLike.getCustomLineItems().forEach(customLineItem -> {
            it.add(goods.toString());
            final String customItemName = localizeOrFallback(customLineItem.getName(), localesList, "custom item");
            id.add(customItemName);
            pr.add(getCentsAmount((customLineItem.getMoney())));
            no.add(customLineItem.getQuantity());
            de.add(customItemName);
            va.add(getTaxRateIfPresentOrZero(customLineItem.getTaxRate()));

            discountAccumulator.addAndGet(getTotalDiscountedPricePerItem(customLineItem));
        });


        // TODO: ensure shippingMethod is expanded in the request!!!
        ofNullable(cartLike.getShippingInfo()).ifPresent(shippingInfo -> {
            it.add(shipment.toString());

            final String shippingMethodName = shippingInfo.getShippingMethodName();
            id.add(shippingMethodName);

            pr.add(getCentsAmount(shippingInfo.getPrice()));
            no.add(1L); // always one shipment item so far

            de.add(ofNullable(shippingInfo.getShippingMethod())
                    .map(Reference::getObj)
                    .map(ShippingMethod::getDescription)
                    .orElse("shipping " + shippingMethodName));

            va.add(getTaxRateIfPresentOrZero(shippingInfo.getTaxRate()));

            ofNullable(shippingInfo.getDiscountedPrice())
                    .map(DiscountedLineItemPrice::getValue)
                    .map(BaseKlarnaRequest::getCentsAmount)
                    .ifPresent(discountAccumulator::addAndGet);
        });

        int totalDiscount = discountAccumulator.get();
        if (totalDiscount > 0) {
            it.add(voucher.toString());
            final String totalDiscountName = "total discount";
            id.add(totalDiscountName);
            pr.add(totalDiscount);

            // so far we count accumulated discount from all items and shipment.
            // Could be changed in future to support separated discount lines in the request
            no.add(1L);

            de.add(totalDiscountName);
            va.add(0.0); // taxes doesn't have any sense for accumulated discount
        }

    }

    @Nullable
    private static String localizeOrFallback(@Nullable LocalizedString string,
                                             @Nonnull Iterable<Locale> locales,
                                             @Nullable String fallback) {
        return ofNullable(string).map(str -> str.get(locales)).orElse(fallback);
    }

    private static double getTaxRateIfPresentOrZero(@Nullable TaxRate taxRate) {
        return ofNullable(taxRate)
                .map(TaxRate::getAmount)
                .map(amount -> amount * 100.0)// CTP stores tax in [0..1] range, Payone requires in [0..100]
                .orElse(0.0);
    }

    private static int getCentsAmount(@Nonnull MonetaryAmount monetaryAmount) {
        return monetaryAmount.getNumber().intValue();
    }

    private static int getTotalDiscountedPricePerItem(@Nonnull LineItemLike lineItemLike) {
        return lineItemLike.getDiscountedPricePerQuantity().stream()
                .mapToInt(discount -> getCentsAmount(discount.getDiscountedPrice().getValue()))
                .sum();
    }

}
