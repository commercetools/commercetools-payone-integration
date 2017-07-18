package com.commercetools.pspadapter.payone.util;

import io.sphere.sdk.carts.LineItem;
import io.sphere.sdk.carts.LineItemLike;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.taxcategories.TaxRate;
import org.javamoney.moneta.Money;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MoneyUtilTest {

    private static final String RESOURCES_ROOT = "com/commercetools/pspadapter/payone/util/";
    private static final String dummyLineItemWithDiscount = RESOURCES_ROOT + "dummyLineItemWithDiscount.json";
    private static final String dummyLineItemWithoutDiscount = RESOURCES_ROOT + "dummyLineItemWithoutDiscount.json";


    @Test
    public void getTaxRateIfPresentOrZero() throws Exception {
        assertThat(MoneyUtil.getTaxRateIfPresentOrZero(null)).isEqualTo(0);

        TaxRate taxRate = mock(TaxRate.class);
        assertThat(MoneyUtil.getTaxRateIfPresentOrZero(taxRate)).isEqualTo(0);
        when(taxRate.getAmount()).thenReturn(0.22);
        assertThat(MoneyUtil.getTaxRateIfPresentOrZero(taxRate)).isEqualTo(22);
    }

    @Test
    public void getActualPricePerLineItem() throws Exception {
        LineItemLike lineItemLike = mock(LineItemLike.class);
        when(lineItemLike.getTotalPrice()).thenReturn(Money.of(42.5, "EUR"));
        assertThat(MoneyUtil.getActualPricePerLineItem(lineItemLike)).isEqualTo(4250);

        lineItemLike = SphereJsonUtils.readObjectFromResource(dummyLineItemWithDiscount, LineItem.class);
        assertThat(MoneyUtil.getActualPricePerLineItem(lineItemLike)).isEqualTo(15700);

        lineItemLike = SphereJsonUtils.readObjectFromResource(dummyLineItemWithoutDiscount, LineItem.class);
        assertThat(MoneyUtil.getActualPricePerLineItem(lineItemLike)).isEqualTo(17700);
    }

    @Test
    public void getTotalDiscountedPricePerQuantity() throws Exception {
        LineItemLike lineItemLike = mock(LineItemLike.class);
        when(lineItemLike.getTotalPrice()).thenReturn(Money.of(42.5, "EUR"));
        assertThat(MoneyUtil.getTotalDiscountedPricePerQuantity(lineItemLike)).isEmpty();

        lineItemLike = SphereJsonUtils.readObjectFromResource(dummyLineItemWithDiscount, LineItem.class);
        assertThat(MoneyUtil.getTotalDiscountedPricePerQuantity(lineItemLike)).hasValue(15700L);

        lineItemLike = SphereJsonUtils.readObjectFromResource(dummyLineItemWithoutDiscount, LineItem.class);
        assertThat(MoneyUtil.getTotalDiscountedPricePerQuantity(lineItemLike)).isEmpty();
    }
}