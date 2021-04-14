package com.commercetools.pspadapter.payone.domain.payone.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author fhaertig
 * @since 17.12.15
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Notification implements Serializable {

    private String key;

    private NotificationAction txaction;

    @JsonProperty("transaction_status")
    private TransactionStatus transactionStatus;

    private String mode;

    private String portalid;

    private String aid;

    private String clearingtype;

    private String txtime;

    private String currency;

    private String userid;

    private String country;

    private String txid;

    private String reference;

    private String sequencenumber;

    private String price;

    private String receivable;

    private String balance;

    private String productid;

    private String accessid;


    public static Notification fromKeyValueString(final String keyValueString, final String separatorPattern) {

        //This code creates a map from an input string like this:
        //                "key=123&" +
        //                "txaction=appointed&" +
        //                "blabla=23"
        //while it splits the string using the separatorPattern (in this case its "\r?\n?&") into substrings
        //which are splitted again at the equality-sign and collected then as key-value pairs into a map
        //throws IllegalArgumentException on duplicate keys.
        final Map<String, String> notificationValues = Pattern.compile(separatorPattern)
               .splitAsStream(keyValueString.trim())
               .map(s -> s.split("="))
               .collect(Collectors.toMap(a -> a[0], a -> a.length>1? a[1]: "",
                   (oldKey, newKey) -> {throw new IllegalArgumentException(oldKey + " is not a valid entry.");}));

        validateNotificationValues(notificationValues);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.convertValue(notificationValues, Notification.class);
    }

    private static void validateNotificationValues(final Map<String, String> notificationValues) {
        if(notificationValues == null || notificationValues.isEmpty()) {
            throw new IllegalArgumentException("The notification string is null or empty.");
        }

        notificationValues.values()
                          .stream()
                          .filter(val -> !StringUtils.isBlank(val))
                          .findFirst()
                          .orElseThrow(IllegalArgumentException::new);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "accessid='" + accessid + '\'' +
                ", productid='" + productid + '\'' +
                ", price='" + price + '\'' +
                ", sequencenumber='" + sequencenumber + '\'' +
                ", reference='" + reference + '\'' +
                ", txid='" + txid + '\'' +
                ", country='" + country + '\'' +
                ", userid='" + userid + '\'' +
                ", currency='" + currency + '\'' +
                ", txtime='" + txtime + '\'' +
                ", clearingtype='" + clearingtype + '\'' +
                ", mode='" + mode + '\'' +
                ", transactionStatus=" + transactionStatus +
                ", txaction=" + txaction +
                '}';
    }

    //**************************************************************
    //* GETTER AND SETTER METHODS
    //**************************************************************

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public NotificationAction getTxaction() {
        return txaction;
    }

    public void setTxaction(final NotificationAction txaction) {
        this.txaction = txaction;
    }

    public TransactionStatus getTransactionStatus() {
        return transactionStatus != null ? transactionStatus : TransactionStatus.COMPLETED;
    }

    public void setTransactionStatus(final TransactionStatus transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(final String mode) {
        this.mode = mode;
    }

    public String getPortalid() {
        return portalid;
    }

    public void setPortalid(final String portalid) {
        this.portalid = portalid;
    }

    public String getAid() {
        return aid;
    }

    public void setAid(final String aid) {
        this.aid = aid;
    }

    public String getClearingtype() {
        return clearingtype;
    }

    public void setClearingtype(final String clearingtype) {
        this.clearingtype = clearingtype;
    }

    public String getTxtime() {
        return txtime;
    }

    public void setTxtime(final String txtime) {
        this.txtime = txtime;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(final String userid) {
        this.userid = userid;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    public String getTxid() {
        return txid;
    }

    public void setTxid(final String txid) {
        this.txid = txid;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(final String reference) {
        this.reference = reference;
    }

    public String getSequencenumber() {
        return sequencenumber;
    }

    public void setSequencenumber(final String sequencenumber) {
        this.sequencenumber = sequencenumber;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(final String price) {
        this.price = price;
    }

    public String getReceivable() {
        return receivable;
    }

    public void setReceivable(final String receivable) {
        this.receivable = receivable;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(final String balance) {
        this.balance = balance;
    }

    public String getProductid() {
        return productid;
    }

    public void setProductid(final String productid) {
        this.productid = productid;
    }

    public String getAccessid() {
        return accessid;
    }

    public void setAccessid(final String accessid) {
        this.accessid = accessid;
    }
}
