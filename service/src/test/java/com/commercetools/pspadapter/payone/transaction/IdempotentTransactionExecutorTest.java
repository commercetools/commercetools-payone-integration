package com.commercetools.pspadapter.payone.transaction;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.TypeCacheLoader;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.queries.TypeQuery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import util.PaymentTestHelper;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static util.JvmSdkMockUtil.pagedQueryResultsMock;

/**
 * @author fhaertig
 * @since 18.01.16
 */
@RunWith(MockitoJUnitRunner.class)
public class IdempotentTransactionExecutorTest {

    private static final Cart UNUSED_CART = null;

    private static final PaymentTestHelper testHelper = new PaymentTestHelper();

    private TestIdempotentTransactionExecutor testee;

    @Mock
    private BlockingSphereClient client;

    @Before
    public void setUp() {
        when(client.executeBlocking(any(TypeQuery.class))).then(a -> {
            PagedQueryResult<Type> customTypes = testHelper.getCustomTypes();
            String queryString = Arrays.asList(a.getArguments()).get(0).toString();
            if (queryString.contains(CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE)) {
                return pagedQueryResultsMock(findCustomTypeByKey(CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE, customTypes));
            } else if (queryString.contains(CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION)) {
                return pagedQueryResultsMock(findCustomTypeByKey(CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION, customTypes));
            } else if (queryString.contains(CustomTypeBuilder.PAYONE_INTERACTION_REDIRECT)) {
                return pagedQueryResultsMock(findCustomTypeByKey(CustomTypeBuilder.PAYONE_INTERACTION_REDIRECT, customTypes));
            } else if (queryString.contains(CustomTypeBuilder.PAYONE_INTERACTION_REQUEST)) {
                return pagedQueryResultsMock(findCustomTypeByKey(CustomTypeBuilder.PAYONE_INTERACTION_REQUEST, customTypes));
            }
            return PagedQueryResult.empty();
        });

        testee = new TestIdempotentTransactionExecutor(CacheBuilder.newBuilder().build(new TypeCacheLoader(client)));
    }

    @Test
    public void nextSequenceNumber0() throws Exception {
        Payment payment = testHelper.dummyPaymentCreatedByNotification();
        payment.getTransactions().clear();
        payment.getInterfaceInteractions().clear();
        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, UNUSED_CART);

        int sequenceNumber = testee.getNextSequenceNumber(paymentWithCartLike);

        assertThat(sequenceNumber).isEqualTo(0);

    }

    @Test
    public void nextSequenceNumber1() throws Exception {
        Payment payment = testHelper.dummyPaymentCreatedByNotification();
        payment.getTransactions().clear();
        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, UNUSED_CART);

        int sequenceNumber = testee.getNextSequenceNumber(paymentWithCartLike);

        assertThat(sequenceNumber).isEqualTo(1);

    }

    @Test
    public void nextSequenceNumber3() throws Exception {
        Payment payment = testHelper.dummyPaymentTwoTransactionsSuccessPending();
        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, UNUSED_CART);

        int sequenceNumber = testee.getNextSequenceNumber(paymentWithCartLike);

        assertThat(sequenceNumber).isEqualTo(3);

    }

    private class TestIdempotentTransactionExecutor extends IdempotentTransactionExecutor {

        public TestIdempotentTransactionExecutor(final LoadingCache<String, Type> typeCache) {
            super(typeCache);
        }

        @Override
        protected int getNextSequenceNumber(final PaymentWithCartLike paymentWithCartLike) {
            return super.getNextSequenceNumber(paymentWithCartLike);
        }

        @Override
        public TransactionType supportedTransactionType() {
            return null;
        }

        @Override
        protected boolean wasExecuted(final PaymentWithCartLike paymentWithCartLike, final Transaction transaction) {
            return false;
        }

        @Override
        protected PaymentWithCartLike executeIdempotent(final PaymentWithCartLike paymentWithCartLike, final Transaction transaction) {
            return null;
        }
    }

    private Type findCustomTypeByKey(String key, PagedQueryResult<Type> customTypes) {
        return customTypes
                .getResults()
                .stream()
                .filter(p -> p.getKey().equals(key))
                .findFirst()
                .get();
    }
}
