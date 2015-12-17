package com.commercetools.pspadapter.payone.domain.payone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import spark.Request;

import java.io.IOException;

/**
 * @author fhaertig
 * @date 17.12.15
 */
@RunWith(MockitoJUnitRunner.class)
public class PayoneNotificationServiceTest {

    @Mock
    private Request request;


    @Test
    public void processPayoneAppointedStatus() throws IOException {
        PayoneNotificationService notificationService = new PayoneNotificationService();

        String requestBody =
                "key=123&" +
                "txaction=appointed&" +
                "transaction_status=completed&" +
                "mode=test&" +
                "portalid=000&" +
                "aid=001&" +
                "clearingtype=cc&" +
                "";

        when(request.body()).thenReturn(requestBody);

        Notification notification = notificationService.receiveNotification(request);

        assertThat(notification.getKey()).isEqualTo("123");
        assertThat(notification.getTxaction()).isEqualTo("appointed");
        assertThat(notification.getTransaction_status()).isEqualTo("completed");
        assertThat(notification.getMode()).isEqualTo("test");
        assertThat(notification.getPortalid()).isEqualTo("000");
        assertThat(notification.getAid()).isEqualTo("001");
        assertThat(notification.getClearingtype()).isEqualTo("cc");
    }
}