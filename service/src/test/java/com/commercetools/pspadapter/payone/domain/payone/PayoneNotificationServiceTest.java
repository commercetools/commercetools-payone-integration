package com.commercetools.pspadapter.payone.domain.payone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
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

    // EXAMPLE:
    // Received POST from Payone: key=<...>&txaction=appointed&portalid=2022125&aid=<...>&clearingtype=cc
    // &notify_version=7.4&txtime=1450365542&currency=EUR&userid=76656077&accessname=&accesscode=&param=&mode=test&price=2000.00
    // &txid=<...>&reference=1448229771690&sequencenumber=0&company=&firstname=&lastname=Test+Buyer&street=nullOptional.empty
    // &zip=&city=&email=&country=DE&cardexpiredate=1703&cardtype=V&cardpan=411111xxxxxx1111&transaction_status=completed&balance=0.00&receivable=0.00

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
                "cardpan=1";

        when(request.body()).thenReturn(requestBody);

        Notification notification = notificationService.receiveNotification(request);

        assertThat(notification.getKey()).isEqualTo("123");
        assertThat(notification.getTxaction()).isEqualTo(NotificationAction.APPOINTED);
        assertThat(notification.getTransaction_status()).isEqualTo("completed");
        assertThat(notification.getMode()).isEqualTo("test");
        assertThat(notification.getPortalid()).isEqualTo("000");
        assertThat(notification.getAid()).isEqualTo("001");
        assertThat(notification.getClearingtype()).isEqualTo("cc");

        //ignores cardpan as unknown
    }
}