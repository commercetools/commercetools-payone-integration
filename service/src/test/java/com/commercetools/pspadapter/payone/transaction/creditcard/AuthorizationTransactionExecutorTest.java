package com.commercetools.pspadapter.payone.transaction.creditcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.mapping.PayoneRequestFactory;
import com.google.common.cache.LoadingCache;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.types.Type;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import util.PaymentTestHelper;


/**
 * @author fhaertig
 * @since 13.01.16
 */
@RunWith(MockitoJUnitRunner.class)
@Ignore
public class AuthorizationTransactionExecutorTest {

    @Mock
    private PaymentWithCartLike paymentWithCartLike;

    @Mock
    private PayoneRequestFactory requestFactory;

    @Mock
    private PayonePostService postService;

    private LoadingCache<String, Type> typeCache;

    @Mock
    private CommercetoolsClient client;

    private final PaymentTestHelper testHelper = new PaymentTestHelper();

    @Before
    public void setUp() {

        //TODO: provide valid custom type references!

        //typeCache.put(CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE, (Type) Reference.of(CustomTypeBuilder.PAYONE_INTERACTION_REDIRECT, "<id>").getObj());
        //typeCache.put(CustomTypeBuilder.PAYONE_INTERACTION_REDIRECT, Type.typeReference());
    }

    @Test
    public void wasExecuted() throws Exception {
        Payment payment = testHelper.dummyPaymentTwoTransactionsSuccessPending();
        Transaction transaction = payment.getTransactions().stream().filter(t -> t.getState().equals(TransactionState.PENDING)).findFirst().get();
        when(paymentWithCartLike.getPayment()).thenReturn(payment);

        AuthorizationTransactionExecutor executor = new AuthorizationTransactionExecutor(
                typeCache,
                requestFactory,
                postService,
                client
        );

        assertThat(executor.wasExecuted(paymentWithCartLike, transaction)).isTrue();
    }
}