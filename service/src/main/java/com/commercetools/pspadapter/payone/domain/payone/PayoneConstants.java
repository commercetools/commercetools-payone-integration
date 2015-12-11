package com.commercetools.pspadapter.payone.domain.payone;

import java.util.Arrays;
import java.util.List;

public final class PayoneConstants {

    public static final String DEFAULT_SHIPPING_PROVIDER = "DHL";

    private PayoneConstants() {
        //Make sure that class of constants has no public constructor
    }

    /**
     * List of all parameters which has to be hashed.
     * **/
    public static final List<String> HASHABLE_PARAMS = Arrays.asList("aid", "mid", "portalid", "mode", "request", "clearingtype", "reference", "customerid",
            "param", "narrative_text", "successurl", "errorurl", "backurl", "storecarddata", "responsetype", "encoding", "display_name", "display_address",
            "autosubmit", "targetwindow", "amount", "currency", "due_time", "invoiceid", "invoiceappendix", "invoice_deliverymode", "eci", "id", "pr", "no",
            "de", "ti", "va", "checktype", "addresschecktype", "consumerscoretype", "productid", "accessname", "accesscode", "access_expiretime",
            "access_canceltime", "access_starttime", "access_period", "access_aboperiod", "access_price", "access_aboprice", "access_vat", "settleperiod",
            "settletime", "vaccountname", "vreference", "userid", "exiturl");

    public final class Parameter {
        public static final String STATUS = "status";
        public static final String REFERENCEID = "referenceid";
        public static final String PAYMENTPROVIDER = "paymentprovider";
        public static final String REDIRECT_URL = "redirecturl";
        private Parameter() {
            //Make sure that class of constants has no public constructor
        }
    }

    public final class Error {
        public static final String PAYMENT_FAILED_COMMON = "payone.common.payment.error";
        private Error() {
            //Make sure that class of constants has no public constructor
        }
    }
}