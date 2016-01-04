package com.commercetools.pspadapter.payone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.domain.payone.model.common.TransactionStatus;
import org.junit.Test;

import java.io.IOException;

/**
 * @author fhaertig
 * @date 17.12.15
 */
public class NotificationTest {


    // EXAMPLE:
    // Received POST from Payone: key=<...>&txaction=appointed&portalid=2022125&aid=<...>&clearingtype=cc
    // &notify_version=7.4&txtime=1450365542&currency=EUR&userid=76656077&accessname=&accesscode=&param=&mode=test&price=2000.00
    // &txid=<...>&reference=1448229771690&sequencenumber=0&company=&firstname=&lastname=Test+Buyer&street=nullOptional.empty
    // &zip=&city=&email=&country=DE&cardexpiredate=1703&cardtype=V&cardpan=411111xxxxxx1111&transaction_status=completed&balance=0.00&receivable=0.00

    @Test
    public void deserializeValidNotification() throws IOException {

        String requestBody =
                "key=123&" +
                "txaction=appointed&" +
                "transaction_status=completed&" +
                "mode=test&" +
                "portalid=000&" +
                "aid=001&" +
                "sequencenumber=1&" +
                "clearingtype=cc&" +
                "cardpan=1&" +
                "blabla=23";   //ignored parameter

        Notification notification = Notification.fromKeyValueString(requestBody, "\r?\n?&");

        assertThat(notification.getKey()).isEqualTo("123");
        assertThat(notification.getTxaction()).isEqualTo(NotificationAction.APPOINTED);
        assertThat(notification.getTransactionStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(notification.getMode()).isEqualTo("test");
        assertThat(notification.getPortalid()).isEqualTo("000");
        assertThat(notification.getAid()).isEqualTo("001");
        assertThat(notification.getClearingtype()).isEqualTo("cc");
        assertThat(notification.getSequencenumber()).isEqualTo("1");
    }

    @Test
    public void throwExceptionForInvalidNotification() throws IOException {

        String requestBody =
                "thisIsNotAValidNotificationString";

        final Throwable noInterface = catchThrowable(() -> Notification.fromKeyValueString(requestBody, "\r?\n?&"));
        assertThat(noInterface).isInstanceOf(IllegalArgumentException.class);
        assertThat(noInterface).hasMessageEndingWith("is not a valid entry");
    }

    @Test
    public void throwExceptionForEmptyParameter() throws IOException {

        String requestBody =
                "key=123&" +
                "txaction=&" +
                "blabla=23";

        final Throwable noInterface = catchThrowable(() -> Notification.fromKeyValueString(requestBody, "\r?\n?&"));
        assertThat(noInterface).isInstanceOf(IllegalArgumentException.class);
    }
}