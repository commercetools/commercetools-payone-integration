package com.commercetools.pspadapter.payone.domain.payone.model.common;

import com.commercetools.pspadapter.payone.config.PayoneConfig;

/**
 * @author fhaertig
 * @date 15.12.15
 */
public class CaptureRequest extends BaseRequest {

    private String txid;

    private Integer sequencenumber;

    private int amount;

    private String currency;

    private String narrative_text;

    private String capturemode;

    private String invoiceid;

    private String invoice_deliverymode;

    private String invoice_deliverydate;

    private String invoice_deliveryenddate;

    private String invoiceappendix;

    public CaptureRequest(final PayoneConfig config) {
        super(config, RequestType.CAPTURE.getType());
    }

    //**************************************************************
    //* GETTER AND SETTER METHODS
    //**************************************************************


    public String getTxid() {
        return txid;
    }

    public void setTxid(final String txid) {
        this.txid = txid;
    }

    public Integer getSequencenumber() {
        return sequencenumber;
    }

    public void setSequencenumber(final Integer sequencenumber) {
        this.sequencenumber = sequencenumber;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(final int amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    public String getNarrative_text() {
        return narrative_text;
    }

    public void setNarrative_text(final String narrative_text) {
        this.narrative_text = narrative_text;
    }

    public String getCapturemode() {
        return capturemode;
    }

    public void setCapturemode(final String capturemode) {
        this.capturemode = capturemode;
    }

    public String getInvoiceid() {
        return invoiceid;
    }

    public void setInvoiceid(final String invoiceid) {
        this.invoiceid = invoiceid;
    }

    public String getInvoice_deliverymode() {
        return invoice_deliverymode;
    }

    public void setInvoice_deliverymode(final String invoice_deliverymode) {
        this.invoice_deliverymode = invoice_deliverymode;
    }

    public String getInvoice_deliverydate() {
        return invoice_deliverydate;
    }

    public void setInvoice_deliverydate(final String invoice_deliverydate) {
        this.invoice_deliverydate = invoice_deliverydate;
    }

    public String getInvoice_deliveryenddate() {
        return invoice_deliveryenddate;
    }

    public void setInvoice_deliveryenddate(final String invoice_deliveryenddate) {
        this.invoice_deliveryenddate = invoice_deliveryenddate;
    }

    public String getInvoiceappendix() {
        return invoiceappendix;
    }

    public void setInvoiceappendix(final String invoiceappendix) {
        this.invoiceappendix = invoiceappendix;
    }
}
