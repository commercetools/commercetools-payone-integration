package com.commercetools.pspadapter.payone.domain.payone;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static enum RequestType {
        preauthorization("preauthorization"), authorization("authorization"), capture("capture"), refund("refund"), debit("debit"),
        createaccess("createaccess"), vauthorization("vauthorization"), getinvoice("getinvoice"), updateuser("updateuser"),
        updateaccess("updateaccess"), updatereminder("(updatereminder"), creditcardcheck("creditcardcheck"), bankaccountcheck("bankaccountcheck"),
        _3dscheck("3dscheck"), addresscheck("addresscheck"), consumerscore("consumerscore"), callback("callback");

        private final String type;

        private static final Map<String, RequestType> LOOKUP_TYPE = new HashMap<String, RequestType>();

        static {
            for (RequestType p : EnumSet.allOf(RequestType.class)) {
                LOOKUP_TYPE.put(p.getType(), p);
            }
        }

        public static RequestType getRequestType(String type) {
            RequestType requestType = LOOKUP_TYPE.get(type);
            return requestType;
        }

        private RequestType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public static enum PaymentType {
        PAYONE_ELV("PayoneELV", "elv", ""), PAYONE_CC("PayoneCC", "cc", ""), PAYONE_VOR("PayoneVOR", "vor", ""), PAYONE_REC("PayoneREC", "rec", ""), PAYONE_COD(
                "PayoneCOD", "cod", ""), PAYONE_PP("PayonePP", "wlt", ""), PAYONE_SU("PayoneSU", "sb", "PNT"), PAYONE_EPS("PayoneEPS", "sb", "EPS"), PAYONE_GP(
                "PayoneGP", "sb", "GPY"), PAYONE_PFF("PayonePFF", "sb", "PFF"), PAYONE_PFC("PayonePFC", "sb", "PFC"), PAYONE_BSV("PayoneBSV", "fnc", "BSV");

        private final String type;
        /**
         * <code>
         * elv: Lastschrift
         * cc: Kreditkarte
         * vor: Vorkasse
         * rec: Rechnung
         * cod: Nachnahme
         * sb: Online-Überweisung
         * wlt: e-Wallet
         * fnc: Financing
         * </code>
         * **/
        private final String code;

        /**
         * <code>
         * PNT: SOFORT-Überweisung (DE, AT,CH)
         * GPY: giropay (DE)
         * EPS: eps – Online-Überweisung (AT)
         * PFF: PostFinance E-Finance (CH)
         * PFC: PostFinance Card (C
         * BSV: Billsafe
         * </code>
         * **/
        private final String subType;
        private static final Map<String, PaymentType> LOOKUP_TYPE = new HashMap<String, PaymentType>();
        private static final Map<String, PaymentType> LOOKUP_CODE = new HashMap<String, PaymentType>();

        static {
            for (PaymentType p : EnumSet.allOf(PaymentType.class)) {
                LOOKUP_TYPE.put(p.getType(), p);
                LOOKUP_CODE.put(p.getCode(), p);
            }
        }

        public static PaymentType getPaymentByType(String type) {
            PaymentType paymentType = null;
            // Same paymentmodes could have different codes, when they have for example different paymentcosts. Because
            // of that only the prefix of each paymentmode is checked
            for (String lookupPaymentType : LOOKUP_TYPE.keySet()) {
                if (StringUtils.containsIgnoreCase(type, lookupPaymentType)) {
                    paymentType = LOOKUP_TYPE.get(lookupPaymentType);
                    break;
                }
            }
            return paymentType;
        }

        public static PaymentType getPaymentByTypeCode(String code) {
            PaymentType paymentType = null;
            // Same paymentmodes could have different Codes, when they have for example different paymentcosts. Because
            // of that only the prefix of each paymentmode is checked
            for (String lookupPaymentCode : LOOKUP_CODE.keySet()) {
                if (StringUtils.containsIgnoreCase(code, lookupPaymentCode)) {
                    paymentType = LOOKUP_CODE.get(lookupPaymentCode);
                    break;
                }
            }

            return paymentType;
        }

        private PaymentType(String type, String code, String subType) {
            this.type = type;
            this.code = code;
            this.subType = subType;
        }

        public String getType() {
            return type;
        }

        public String getCode() {
            return code;
        }

        public String getSubType() {
            return subType;
        }
    }

    public final class Parameter {
        public static final String STATUS = "status";
        public static final String REFERENCEID = "referenceid";
        public static final String PAYMENTPROVIDER = "paymentprovider";
        public static final String REDIRECT_URL = "redirecturl";
        private Parameter() {
            //Make sure that class of constants has no public constructor
        }
    }

    public final class State {
        public static final String REDIRECT = "REDIRECT";
        public static final String APPROVED = "APPROVED";
        public static final String ERROR = "ERROR";
        private State() {
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