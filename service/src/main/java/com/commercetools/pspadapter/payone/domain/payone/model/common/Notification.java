package com.commercetools.pspadapter.payone.domain.payone.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;

import java.io.Serializable;
import java.util.Map;

/**
 * @author fhaertig
 * @date 17.12.15
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

    private String productid;

    private String accessid;


    public static Notification fromKeyValueString(final String keyValueString, final String separatorPattern) {

        Map<String, String> notificationValues = Splitter.onPattern(separatorPattern).withKeyValueSeparator("=").split(keyValueString);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.convertValue(notificationValues, Notification.class);
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
        return transactionStatus;
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
