package com.commercetools.pspadapter.payone.domain.payone.model;

import org.apache.commons.lang3.StringUtils;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fhaertig
 * @date 11.12.15
 */
public enum PaymentType {
    PAYONE_ELV("PayoneELV", "elv", ""),
    PAYONE_CC("PayoneCC", "cc", ""),
    PAYONE_VOR("PayoneVOR", "vor", ""),
    PAYONE_REC("PayoneREC", "rec", ""),
    PAYONE_COD("PayoneCOD", "cod", ""),
    PAYONE_PP("PayonePP", "wlt", ""),
    PAYONE_SU("PayoneSU", "sb", "PNT"),
    PAYONE_EPS("PayoneEPS", "sb", "EPS"),
    PAYONE_GP("PayoneGP", "sb", "GPY"),
    PAYONE_PFF("PayonePFF", "sb", "PFF"),
    PAYONE_PFC("PayonePFC", "sb", "PFC"),
    PAYONE_BSV("PayoneBSV", "fnc", "BSV");

    private final String key;
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
            LOOKUP_TYPE.put(p.getKey(), p);
            LOOKUP_CODE.put(p.getCode(), p);
        }
    }

    private PaymentType(final String key, final String code, final String subType) {
        this.key = key;
        this.code = code;
        this.subType = subType;
    }

    public static PaymentType getPaymentTypeByKey(final String key) {
        PaymentType paymentType = null;
        // Same paymentmodes could have different codes, when they have for example different paymentcosts. Because
        // of that only the prefix of each paymentmode is checked
        for (String lookupPaymentType : LOOKUP_TYPE.keySet()) {
            if (StringUtils.containsIgnoreCase(key, lookupPaymentType)) {
                paymentType = LOOKUP_TYPE.get(lookupPaymentType);
                break;
            }
        }
        return paymentType;
    }

    public static PaymentType getPaymentTypeByCode(final String code) {
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

    public String getKey() {
        return key;
    }

    public String getCode() {
        return code;
    }

    public String getSubType() {
        return subType;
    }
}
