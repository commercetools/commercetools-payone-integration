package com.commercetools.pspadapter.payone.domain.payone.model.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import java.io.IOException;

/**
 * @author fhaertig
 * @author Jan Wolter
 * @since 17.12.15
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
                "txtime=1450365542&" +
                "cardpan=1&" +
                "blabla=23";   //ignored parameter

        Notification notification = Notification.fromKeyValueString(requestBody, "\r?\n?&");

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(notification.getKey()).as("key").isEqualTo("123");
        softly.assertThat(notification.getTxaction()).as("txaction").isEqualTo(NotificationAction.APPOINTED);
        softly.assertThat(notification.getTransactionStatus()).as("transaction_status").isEqualTo(TransactionStatus.COMPLETED);
        softly.assertThat(notification.getMode()).as("mode").isEqualTo("test");
        softly.assertThat(notification.getPortalid()).as("portalid").isEqualTo("000");
        softly.assertThat(notification.getAid()).as("aid").isEqualTo("001");
        softly.assertThat(notification.getClearingtype()).as("clearingtype").isEqualTo("cc");
        softly.assertThat(notification.getSequencenumber()).as("sequencenumber").isEqualTo("1");
        softly.assertThat(notification.getTxtime()).as("txtime").isEqualTo("1450365542");

        softly.assertAll();
    }

    @Test
    public void deserializeValidNotificationWithTransactionStatusPending() throws IOException {

        final String requestBody =
                "key=123&" +
                "txaction=appointed&" +
                "transaction_status=pending&" +
                "mode=test&" +
                "portalid=000&" +
                "aid=001&" +
                "sequencenumber=1&" +
                "clearingtype=cc&" +
                "txtime=1450365542&" +
                "cardpan=1&" +
                "price=123,45&" +
                "receivable=0,00&" +
                "balance=98,76&" +
                "blabla=23";   //ignored parameter

        final Notification notification = Notification.fromKeyValueString(requestBody, "\r?\n?&");

        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(notification.getKey()).as("key").isEqualTo("123");
        softly.assertThat(notification.getTxaction()).as("txaction").isEqualTo(NotificationAction.APPOINTED);
        softly.assertThat(notification.getTransactionStatus()).as("transaction_status").isEqualTo(TransactionStatus.PENDING);
        softly.assertThat(notification.getMode()).as("mode").isEqualTo("test");
        softly.assertThat(notification.getPortalid()).as("portalid").isEqualTo("000");
        softly.assertThat(notification.getAid()).as("aid").isEqualTo("001");
        softly.assertThat(notification.getClearingtype()).as("clearingtype").isEqualTo("cc");
        softly.assertThat(notification.getSequencenumber()).as("sequencenumber").isEqualTo("1");
        softly.assertThat(notification.getTxtime()).as("txtime").isEqualTo("1450365542");
        softly.assertThat(notification.getPrice()).as("price").isEqualTo("123,45");
        softly.assertThat(notification.getReceivable()).as("receivable").isEqualTo("0,00");
        softly.assertThat(notification.getBalance()).as("balance").isEqualTo("98,76");

        softly.assertAll();
    }

    @Test
    public void deserializeValidNotificationWithoutTransactionStatus() throws IOException {

        final String requestBody =
                "key=123&" +
                "txaction=appointed&" +
                "mode=test&" +
                "portalid=000&" +
                "aid=001&" +
                "sequencenumber=1&" +
                "clearingtype=cc&" +
                "txtime=1450365542&" +
                "cardpan=1&" +
                "blabla=23";   //ignored parameter

        final Notification notification = Notification.fromKeyValueString(requestBody, "\r?\n?&");

        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(notification.getKey()).as("key").isEqualTo("123");
        softly.assertThat(notification.getTxaction()).as("txaction").isEqualTo(NotificationAction.APPOINTED);
        softly.assertThat(notification.getTransactionStatus()).as("transaction_status").isEqualTo(TransactionStatus.COMPLETED);
        softly.assertThat(notification.getMode()).as("mode").isEqualTo("test");
        softly.assertThat(notification.getPortalid()).as("portalid").isEqualTo("000");
        softly.assertThat(notification.getAid()).as("aid").isEqualTo("001");
        softly.assertThat(notification.getClearingtype()).as("clearingtype").isEqualTo("cc");
        softly.assertThat(notification.getSequencenumber()).as("sequencenumber").isEqualTo("1");
        softly.assertThat(notification.getTxtime()).as("txtime").isEqualTo("1450365542");

        softly.assertAll();
    }

    @Test
    public void throwExceptionForInvalidNotification() throws IOException {

        String requestBody = "=123&=y";

        final Throwable noInterface = catchThrowable(() -> Notification.fromKeyValueString(requestBody, "\r?\n?&"));
        assertThat(noInterface).isInstanceOf(IllegalArgumentException.class);
        assertThat(noInterface).hasMessageEndingWith("is not a valid entry.");
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