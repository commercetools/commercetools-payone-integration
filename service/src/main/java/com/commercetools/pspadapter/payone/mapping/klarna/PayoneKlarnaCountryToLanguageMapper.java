package com.commercetools.pspadapter.payone.mapping.klarna;

import com.commercetools.pspadapter.payone.mapping.CountryToLanguageMapper;
import com.google.common.collect.ImmutableMap;
import com.neovisionaries.i18n.CountryCode;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.neovisionaries.i18n.CountryCode.*;
import static java.util.Locale.GERMAN;
import static java.util.Optional.ofNullable;

/**
 * Country to language mapper implementation for Payone/Klarna requirements. The countries are mapped as it described in
 * <i>PAYONE_Platform_Klarna_Addon_EN_2016-11-30</i> documentation.
 */
public class PayoneKlarnaCountryToLanguageMapper implements CountryToLanguageMapper {

    private static final Map<CountryCode, Locale> COUNTRY_LANGUAGE;

    static {
        final Locale finish = new Locale("fi");
        COUNTRY_LANGUAGE = ImmutableMap.<CountryCode, Locale>builder()
                .put(SE, new Locale("sv"))
                .put(NO, new Locale("nb"))
                .put(FI, finish) // Finland has 2 country codes
                .put(SF, finish)
                .put(DK, new Locale("da"))
                .put(NL, new Locale("nl"))
                .put(DE, GERMAN)
                .put(AT, GERMAN)
                .build();
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
