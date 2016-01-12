package com.commercetools.pspadapter.payone.domain.payone.model.common;

import org.apache.commons.lang3.StringUtils;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fhaertig
 * @since 11.12.15
 */
public enum ClearingType {
    PAYONE_ELV("PayoneELV", "elv", ""),
    PAYONE_CC("CREDIT_CARD", "cc", ""),
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
    private final String payoneCode;

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
    private static final Map<String, ClearingType> LOOKUP_TYPE = new HashMap<>();
    private static final Map<String, ClearingType> LOOKUP_CODE = new HashMap<>();

    static {
        for (ClearingType p : EnumSet.allOf(ClearingType.class)) {
            LOOKUP_TYPE.put(p.getKey(), p);
            LOOKUP_CODE.put(p.getPayoneCode(), p);
        }
    }

    ClearingType(final String key, final String payoneCode, final String subType) {
        this.key = key;
        this.payoneCode = payoneCode;
        this.subType = subType;
    }

    public static ClearingType getClearingTypeByKey(final String key) {
        ClearingType clearingType = null;
        // Same paymentmodes could have different codes, when they have for example different paymentcosts. Because
        // of that only the prefix of each paymentmode is checked
        for (String lookupPaymentType : LOOKUP_TYPE.keySet()) {
            if (StringUtils.containsIgnoreCase(key, lookupPaymentType)) {
                clearingType = LOOKUP_TYPE.get(lookupPaymentType);
                break;
            }
        }
        return clearingType;
    }

    public static ClearingType getClearingTypeByCode(final String payoneCode) {
        ClearingType clearingType = null;
        // Same paymentmodes could have different Codes, when they have for example different paymentcosts. Because
        // of that only the prefix of each paymentmode is checked
        for (String lookupPaymentCode : LOOKUP_CODE.keySet()) {
            if (StringUtils.containsIgnoreCase(payoneCode, lookupPaymentCode)) {
                clearingType = LOOKUP_CODE.get(lookupPaymentCode);
                break;
            }
        }

        return clearingType;
    }

    public String getKey() {
        return key;
    }

    public String getPayoneCode() {
        return payoneCode;
    }

    public String getSubType() {
        return subType;
    }
}
