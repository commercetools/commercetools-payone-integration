package com.commercetools.pspadapter.payone.mapping.klarna;

import com.commercetools.pspadapter.payone.mapping.CountryToLanguageMapper;
import com.neovisionaries.i18n.CountryCode;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import static com.neovisionaries.i18n.CountryCode.*;
import static java.util.Locale.GERMAN;
import static java.util.Locale.forLanguageTag;

/**
 * Dumb tests to protect from accidental wrong changes.
 */
public class PayoneKlarnaCountryToLanguageMapperTest {

    private CountryToLanguageMapper countryToLanguageMapper;

    @Before
    public void setUp() throws Exception {
        countryToLanguageMapper = new PayoneKlarnaCountryToLanguageMapper();
    }

    @Test
    public void mapCountryToLanguage_fromCountryCode() throws Exception {
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage(AT)).contains(GERMAN);
        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage(DE)).contains(GERMAN);
        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage(DK)).contains(forLanguageTag("da"));
        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage(FI)).contains(forLanguageTag("fi"));
        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage(NL)).contains(forLanguageTag("nl"));
        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage(NO)).contains(forLanguageTag("nb"));
        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage(SE)).contains(forLanguageTag("sv"));
        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage(SF)).contains(forLanguageTag("fi"));

        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage(UY)).isEmpty();
        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage((CountryCode) null)).isEmpty();

        softly.assertAll();
    }

    @Test
    public void mapCountryToLanguage_fromCountryCodeString() throws Exception {
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage("AT")).contains(GERMAN);
        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage("DE")).contains(GERMAN);
        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage("DK")).contains(forLanguageTag("da"));
        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage("FI")).contains(forLanguageTag("fi"));

        // and case insensitive
        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage("nl")).contains(forLanguageTag("nl"));
        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage("nO")).contains(forLanguageTag("nb"));
        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage("Se")).contains(forLanguageTag("sv"));
        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage("sf")).contains(forLanguageTag("fi"));

        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage("UY")).isEmpty();
        softly.assertThat(countryToLanguageMapper.mapCountryToLanguage((String)null)).isEmpty();

        softly.assertAll();
    }

}