package com.commercetools.pspadapter.payone.domain.payone.model.klarna;

import javax.annotation.Nonnull;
import java.util.stream.Collector;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collector.Characteristics.IDENTITY_FINISH;
import static java.util.stream.Collector.Characteristics.UNORDERED;

/**
 * Class to calculate/aggregate full and actual prices, and then get discount.
 */
public class PricesAggregator {

    /**
     * Full prices - without discounts
     */
    private long fullCents;

    /**
     * Actual prices - with discounts
     */
    private long actualCents;

    public PricesAggregator() {
        this(0, 0);
    }

    public PricesAggregator(long fullCents, long actualCents) {
        this.fullCents = fullCents;
        this.actualCents = actualCents;
    }

    /**
     * Mutable add prices form {@code that} to <b>this</b> object.
     *
     * @param that object from which to add prices.
     */
    public void accumulatePrices(@Nonnull PricesAggregator that) {
        this.fullCents += that.fullCents;
        this.actualCents += that.actualCents;
    }

    /**
     * Combine 2 prices objects to single one as adding the prices. The original object remains immutable.
     *
     * @param that object to combine with
     * @return a new instance with summed prices.
     */
    public PricesAggregator combinePrices(@Nonnull PricesAggregator that) {
        return new PricesAggregator(this.fullCents + that.fullCents, this.actualCents + that.actualCents);
    }

    /**
     * Calculate discount. Positive value means discount is applied.
     * Negative value likely means something wrong in the workflow (like customer is charged more, than full price).
     *
     * @return discount from the: subtract of full price and actual price.
     * <b>0</b> means no discount could be applicable.
     */
    public long getDiscount() {
        return fullCents - actualCents;
    }


    /**
     * Collector function to collect prices from different sources.
     * Expected to be used in streams.
     *
     * @return new instance for {@link java.util.stream.Stream} collectors.
     */
    public static Collector<PricesAggregator, PricesAggregator, PricesAggregator> pricesCollector() {
        return Collector.of(PricesAggregator::new, PricesAggregator::accumulatePrices, PricesAggregator::combinePrices,
                UNORDERED, IDENTITY_FINISH);
    }

    @Override
    public boolean equals(Object object) {
        return object == this || ofNullable(object)
                .filter(that -> that instanceof PricesAggregator)
                .map(that -> (PricesAggregator) that)
                .map(that -> that.fullCents == this.fullCents && that.actualCents == this.actualCents)
                .orElse(false);
    }

    @Override
    public int hashCode() {
        return (int) (fullCents * 31 + actualCents);
    }


    @Override
    public String toString() {
        return format("PricesAggregator[fullCents=%d, actualCents=%d]", fullCents, actualCents);
    }
}
