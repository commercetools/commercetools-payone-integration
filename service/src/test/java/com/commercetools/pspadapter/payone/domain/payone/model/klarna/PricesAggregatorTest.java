package com.commercetools.pspadapter.payone.domain.payone.model.klarna;

import org.junit.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


public class PricesAggregatorTest {

    @Test
    public void accumulatePrices() throws Exception {
        PricesAggregator pricesAggregator = new PricesAggregator(5, 6);

        pricesAggregator.accumulatePrices(new PricesAggregator());
        assertThat(pricesAggregator).isEqualTo(new PricesAggregator(5, 6));

        pricesAggregator.accumulatePrices(new PricesAggregator(42, 456));
        assertThat(pricesAggregator).isEqualTo(new PricesAggregator(47, 462));

        pricesAggregator.accumulatePrices(new PricesAggregator(-8, -55));
        assertThat(pricesAggregator).isEqualTo(new PricesAggregator(39, 407));

        PricesAggregator immutable = new PricesAggregator(10, 20);
        pricesAggregator.accumulatePrices(immutable);
        assertThat(pricesAggregator).isEqualTo(new PricesAggregator(49, 427));
        // ensure the passing value is not mutated by the method
        assertThat(immutable).isEqualTo(new PricesAggregator(10, 20));
    }

    @Test
    public void combinePrices() throws Exception {
        final PricesAggregator originalPrices = new PricesAggregator(1_000_000_000_000L, 123456789L);
        final PricesAggregator addedPrice1 = new PricesAggregator(10, 20);
        PricesAggregator combinedPrice = originalPrices.combinePrices(addedPrice1);

        assertThat(combinedPrice).isEqualTo(new PricesAggregator(1_000_000_000_010L, 123456809L));
        // ensure immutability
        assertThat(originalPrices).isEqualTo(new PricesAggregator(1_000_000_000_000L, 123456789L));
        assertThat(addedPrice1).isEqualTo(new PricesAggregator(10, 20));

        final PricesAggregator addedPrice2 = new PricesAggregator(-10, -20);
        combinedPrice = originalPrices.combinePrices(addedPrice2);

        assertThat(combinedPrice).isEqualTo(new PricesAggregator(999_999_999_990L, 123456769L));
        // ensure immutability
        assertThat(originalPrices).isEqualTo(new PricesAggregator(1_000_000_000_000L, 123456789L));
        assertThat(addedPrice2).isEqualTo(new PricesAggregator(-10, -20));
    }

    @Test
    public void getDiscount() throws Exception {
        final PricesAggregator originalPrices = new PricesAggregator(456, 123);
        assertThat(originalPrices.getDiscount()).isEqualTo(333);
        originalPrices.accumulatePrices(new PricesAggregator(1, 2));
        assertThat(originalPrices.getDiscount()).isEqualTo(332);

        final PricesAggregator originalPrices2 = new PricesAggregator(123, 456);
        assertThat(originalPrices2.getDiscount()).isEqualTo(-333);
        originalPrices2.accumulatePrices(new PricesAggregator(-1, 2));
        assertThat(originalPrices2.getDiscount()).isEqualTo(-336);
    }

    @Test
    public void pricesCollector() throws Exception {
        assertThat(Stream.of(new PricesAggregator(1, 2),
                new PricesAggregator(5, 6),
                new PricesAggregator(-5, -8),
                new PricesAggregator(48, 1_234_567_890L),
                new PricesAggregator(),
                new PricesAggregator(7, 8))
                .collect(PricesAggregator.pricesCollector())
        )
                .isEqualTo(new PricesAggregator(56, 1234567898));

        assertThat(Stream.of(new PricesAggregator(48, 49)).collect(PricesAggregator.pricesCollector()))
                .isEqualTo(new PricesAggregator(48, 49));

        assertThat(Stream.<PricesAggregator>empty().collect(PricesAggregator.pricesCollector()))
                .isEqualTo(new PricesAggregator(0, 0));
    }

}