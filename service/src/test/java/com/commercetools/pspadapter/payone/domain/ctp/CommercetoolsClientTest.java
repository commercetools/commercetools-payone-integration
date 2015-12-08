package com.commercetools.pspadapter.payone.domain.ctp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.queries.PaymentQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.CompletionStage;

/**
 * @author fhaertig
 * @date 03.12.15
 */
@RunWith(MockitoJUnitRunner.class)
public class CommercetoolsClientTest {

    @Mock
    private SphereClient sphereClient;

    private PaymentQuery paymentSphereRequest = PaymentQuery.of();

    @InjectMocks
    private CommercetoolsClient client;

    @Test
    public void sphereClientRequestDelegation()  {
        @SuppressWarnings("unchecked")
        CompletionStage<PagedQueryResult<Payment>> paymentCompletionStage =
                (CompletionStage<PagedQueryResult<Payment>>) mock(CompletionStage.class);

        when(sphereClient.execute(same(paymentSphereRequest))).thenReturn(paymentCompletionStage);
        assertThat(client.execute(paymentSphereRequest), is(sameInstance(paymentCompletionStage)));
    }

}