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
    PAYONE_CC("CREDIT_CARD", "cc", ""),
    PAYONE_PPE("WALLET-PAYPAL", "wlt", "PPE"),
    PAYONE_PDT("WALLET-PAYDIREKT", "wlt", "PDT"),
    PAYONE_PNT("BANK_TRANSFER-SOFORTUEBERWEISUNG", "sb", "PNT"),
    PAYONE_PFC("BANK_TRANSFER-POSTFINANCE_CARD", "sb", "PFC"),
    PAYONE_PFF("BANK_TRANSFER-POSTFINANCE_EFINANCE", "sb", "PFF"),
    PAYONE_VOR("BANK_TRANSFER-ADVANCE", "vor", ""),
    PAYONE_KLV("INVOICE-KLARNA", "fnc", "KLV");

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
     * PFC: PostFinance Card (CH)
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

    public static ClearingType getClearingTypeByKey(final String ctKey) {
        ClearingType clearingType = null;
        // Same paymentmodes could have different codes, when they have for example different paymentcosts. Because
        // of that only the prefix of each paymentmode is checked
        for (String lookupPaymentType : LOOKUP_TYPE.keySet()) {
            if (StringUtils.containsIgnoreCase(ctKey, lookupPaymentType)) {
                clearingType = LOOKUP_TYPE.get(lookupPaymentType);
                break;
            }
        }
        if (clearingType == null) {
            throw new IllegalArgumentException("commercetools clearingtype '" + ctKey + "' could not be mapped.");
        } else {
            return clearingType;
        }
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

        if (clearingType == null) {
            throw new IllegalArgumentException("Payone clearingtype '" + payoneCode + "' could not be mapped.");
        } else {
            return clearingType;
        }
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
