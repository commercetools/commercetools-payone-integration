package com.commercetools.pspadapter.payone.mapping.klarna;

import com.commercetools.pspadapter.payone.mapping.CountryToLanguageMapper;
import com.neovisionaries.i18n.CountryCode;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.neovisionaries.i18n.CountryCode.AT;
import static com.neovisionaries.i18n.CountryCode.DE;
import static com.neovisionaries.i18n.CountryCode.DK;
import static com.neovisionaries.i18n.CountryCode.FI;
import static com.neovisionaries.i18n.CountryCode.NL;
import static com.neovisionaries.i18n.CountryCode.NO;
import static com.neovisionaries.i18n.CountryCode.SE;
import static com.neovisionaries.i18n.CountryCode.SF;
import static java.util.Locale.GERMAN;
import static java.util.Optional.ofNullable;

/**
 * Country to language mapper implementation for Payone/Klarna requirements. The countries are mapped as it described in
 * <i>PAYONE_Platform_Klarna_Addon_EN_2016-11-30</i> documentation.
 */
public class PayoneKlarnaCountryToLanguageMapper implements CountryToLanguageMapper {

    private static final Map<CountryCode, Locale> COUNTRY_LANGUAGE;

    static {
        final Locale finnish = new Locale("fi");
        final Map<CountryCode, Locale> countryMap = new HashMap<>();
        countryMap.put(SE, new Locale("sv"));
        countryMap.put(NO, new Locale("nb"));
        countryMap.put(FI, finnish); // Finland has 2 country codes
        countryMap.put(SF, finnish);
        countryMap.put(DK, new Locale("da"));
        countryMap.put(NL, new Locale("nl"));
        countryMap.put(DE, GERMAN);
        countryMap.put(AT, GERMAN);
        COUNTRY_LANGUAGE = Collections.unmodifiableMap(countryMap);
    }

    @Override
    public Optional<Locale> mapCountryToLanguage(@Nullable CountryCode countryCode) {
        return ofNullable(countryCode)
                .map(COUNTRY_LANGUAGE::get);
    }

    @Override
    public Optional<Locale> mapCountryToLanguage(@Nullable String countryCode) {
        return mapCountryToLanguage(CountryCode.getByCodeIgnoreCase(countryCode));
    }
}
