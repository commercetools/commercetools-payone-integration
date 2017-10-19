package com.commercetools.pspadapter.payone.util;

import io.sphere.sdk.cartdiscounts.DiscountedLineItemPriceForQuantity;
import io.sphere.sdk.carts.LineItemLike;
import io.sphere.sdk.taxcategories.TaxRate;
import io.sphere.sdk.utils.MoneyImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.money.MonetaryAmount;
import java.util.Optional;

import static io.sphere.sdk.utils.MoneyImpl.centAmountOf;
import static java.util.Optional.ofNullable;

/**
 * Payments/prices/discounts calculation and expanding utils.
 */
public final class MoneyUtil {

    /**
     * Tax rate in Payone compatible value.
     * <p>
     * <b>Note:</b> for now (23.05.2017) Payone accepts tax rate only as an integer,
     * thus value like 19.5% can't be passed.
     * </p>
     *
     * @param taxRate {@link TaxRate} from which to fetch the value
     * @return value of {@link TaxRate#getAmount()} or zero, if {@code taxRate} is empty.
     */
    public static int getTaxRateIfPresentOrZero(@Nullable TaxRate taxRate) {
        return ofNullable(taxRate)
                .map(TaxRate::getAmount)
                .map(amount -> amount * 100)// CTP stores tax in [0..1] range, Payone requires in [0..100]
                .map(Double::intValue) //  PAYONE accepts only integer there!!!
                .orElse(0);
    }

    /**
     * Get real total price (cents) the customer should pay for this line item,
     * accounting the quantity and the discounts.
     * <p>
     * This function "expands" the case, that {@link LineItemLike#getTotalPrice()} is a discounted price,
     * if the discount exists, or full price (e.g., same as discounted) if the discounted price is empty.
     * </p>
     * <p>The function logic is the next:<ul>
     * <li>If {@link LineItemLike#getDiscountedPricePerQuantity()} is not empty - the result will be calculated as
     * an accumulated amount of all {@link DiscountedLineItemPriceForQuantity#getDiscountedPrice()} multiplied
     * to quantity ({@link DiscountedLineItemPriceForQuantity#getQuantity()}</li>
     * <li><i>otherwise</i>, fallback to {@link LineItemLike#getTotalPrice()}, namely the total price is an actual
     * price</li>
     * </ul></p>
     *
     * @param lineItemLike line item from which to calculate the actual price
     * @return cent amount of actual price the customer will pay for whole line item, including the quantity
     * and the discounts
     */
    public static long getActualPricePerLineItem(@Nonnull LineItemLike lineItemLike) {
        return getTotalDiscountedPricePerQuantity(lineItemLike)
                .orElseGet(() -> centAmountOf(lineItemLike.getTotalPrice()));
    }

    /**
     * Get optional total discounted price (cents) of the {@code lineItemLike}, if such a discounted price exists.
     * Otherwise return empty.
     *
     * @param lineItemLike {@link LineItemLike} from which to take the discounted price.
     * @return total discounted price for all <i>quantity</i> items, or empty.
     */
    public static Optional<Long> getTotalDiscountedPricePerQuantity(@Nonnull LineItemLike lineItemLike) {
        return getTotalDiscountedPriceMonetaryPerQuantity(lineItemLike).map(MoneyImpl::centAmountOf);
    }

    public static Optional<MonetaryAmount> getTotalDiscountedPriceMonetaryPerQuantity(@Nonnull LineItemLike lineItemLike) {
        return Optional.ofNullable(lineItemLike.getDiscountedPricePerQuantity())
                .filter(dppq -> dppq.size() > 0)
                .flatMap(dppq -> dppq.stream()
                        .map(dlipfq -> dlipfq.getDiscountedPrice().getValue().multiply(dlipfq.getQuantity()))
                        .reduce(MonetaryAmount::add));
    }

    private MoneyUtil() {
    }
}
