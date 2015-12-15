package com.commercetools.pspadapter.payone.mapping;

import io.sphere.sdk.models.EnumValue;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author fhaertig
 * @date 15.12.15
 */
public enum CreditCardNetwork {

    VISA("Visa", "V"),
    MASTERCARD("MasterCard", "M"),
    DISCOVER("Discover", "C"),
    AMERICAN_EXPRESS("American Express", "A"),
    DINERS_CLUB("Diners Club", "D"),
    JCB("JCB", "J"),
    CARTE_BANCAIRE("CB (Carte Bancaire / Carte Bleue)", "B");

    private String label;
    private String payoneCode;

    CreditCardNetwork(final String label, final String payoneCode) {
        this.label = label;
        this.payoneCode = payoneCode;
    }

    public String getLabel() {
        return label;
    }

    public String getPayoneCode() {
        return payoneCode;
    }

    public static List<EnumValue> getValuesAsListOfEnumValue() {
        List<EnumValue> enumValues =
                EnumSet.allOf(CreditCardNetwork.class)
                        .stream()
                        .map(p -> EnumValue.of(p.name(), p.getLabel()))
                        .collect(Collectors.toList());

        return enumValues;
    }
}
