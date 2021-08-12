package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.types.CustomFields;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.LANGUAGE_CODE_FIELD;
import static com.commercetools.util.ServiceConstants.DEFAULT_LOCALE;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.javamoney.moneta.function.MonetaryQueries.convertMinorPart;

/**
 * @author fhaertig
 * @since 13.12.15
 */
public class MappingUtil {

    private static final Logger LOG = LoggerFactory.getLogger(MappingUtil.class);

    /**
     * Payone birthday format.
     */
    public static final DateTimeFormatter yyyyMMdd = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final Set<CountryCode> countriesWithStateAllowed = new HashSet<>(Arrays.asList(
            CountryCode.US,
            CountryCode.CA,
            CountryCode.CN,
            CountryCode.JP,
            CountryCode.MX,
            CountryCode.BR,
            CountryCode.AR,
            CountryCode.ID,
            CountryCode.TH,
            CountryCode.IN
    ));

    public static void mapBillingAddressToRequest(
            final AuthorizationRequest request,
            final Address billingAddress) {

        if(billingAddress == null) {
            throw new IllegalArgumentException("Missing billing address details");
        }

        //required
        request.setLastname(billingAddress.getLastName());
        request.setCountry(billingAddress.getCountry().toLocale().getCountry());

        //optional
        setIfNonEmpty(request::setTitle, billingAddress.getTitle());
        setIfNonEmpty(request::setSalutation, billingAddress.getSalutation());
        setIfNonEmpty(request::setFirstname, billingAddress.getFirstName());
        setIfNonEmpty(request::setCompany, billingAddress.getCompany());
        setIfNonEmpty(request::setStreet, joinStringsIgnoringNull(Arrays.asList(billingAddress.getStreetName(), billingAddress.getStreetNumber())));
        setIfNonEmpty(request::setAddressaddition, billingAddress.getAdditionalStreetInfo());
        setIfNonEmpty(request::setZip, billingAddress.getPostalCode());
        setIfNonEmpty(request::setCity, billingAddress.getCity());
        setIfNonEmpty(request::setEmail, billingAddress.getEmail());
        setIfNonEmpty(request::setTelephonenumber,
                ofNullable(billingAddress.getPhone())
                        .orElse(billingAddress.getMobile()));

        if (countriesWithStateAllowed.contains(billingAddress.getCountry())) {
            request.setState(billingAddress.getState());
        }
    }

    private static void setIfNonEmpty(Consumer<String> setter, String value) {
        if (StringUtils.isNotBlank(value)) setter.accept(value);
    }

    @Nonnull
    private static String joinStringsIgnoringNull(@Nonnull final List<String> stringsToJoin) {
        return stringsToJoin.stream()
                     .filter(s -> !StringUtils.isBlank(s))
                     .collect(Collectors.joining(" "));
    }

    public static void mapCustomerToRequest(@Nonnull final AuthorizationRequest request,
                                            @Nullable final Reference<Customer> customerReference) {

        if(customerReference == null || customerReference.getObj() == null) {
            throw new IllegalArgumentException("Missing customer object");
        }

        final Customer customer = customerReference.getObj();

        setIfNonEmpty(request::setVatid, customer.getVatId());

        //birthday
        ofNullable(customer.getDateOfBirth())
                .ifPresent(birthday -> request.setBirthday(dateToBirthdayString(birthday)));

        //customerNumber
        ofNullable(customer.getCustomerNumber())
                .ifPresent(customerNumber -> {
                    if (customerNumber.length() > 20) {
                        LOG.warn("customer.customerNumber exceeds the maximum length of 20! Using substring of customer.id as fallback.");
                        String id = customer.getId();
                        id = id.replace("-", "").substring(0, 20);
                        request.setCustomerid(id);
                    } else {
                        request.setCustomerid(customerNumber);
                    }
                });
    }

    public static void mapShippingAddressToRequest(final AuthorizationRequest request, final Address shippingAddress) {

        if(shippingAddress == null) {
            throw new IllegalArgumentException("Missing shipping address details");
        }

        setIfNonEmpty(request::setShipping_firstname, shippingAddress.getFirstName());
        setIfNonEmpty(request::setShipping_lastname, shippingAddress.getLastName());
        setIfNonEmpty(request::setShipping_street, joinStringsIgnoringNull(Arrays.asList(shippingAddress.getStreetName(),
            shippingAddress.getStreetNumber())));
        setIfNonEmpty(request::setShipping_zip, shippingAddress.getPostalCode());
        setIfNonEmpty(request::setShipping_city, shippingAddress.getCity());
        setIfNonEmpty(request::setShipping_country, shippingAddress.getCountry().toLocale().getCountry());
        setIfNonEmpty(request::setShipping_company, joinStringsIgnoringNull(Arrays.asList(shippingAddress.getCompany(),
            shippingAddress.getDepartment())));

        if (countriesWithStateAllowed.contains(shippingAddress.getCountry())) {
            request.setShipping_state(shippingAddress.getState());
        }
    }

    public static void mapCustomFieldsFromPayment(final AuthorizationRequest request, final CustomFields ctPaymentCustomFields) {

        request.setNarrative_text(ctPaymentCustomFields.getFieldAsString(CustomFieldKeys.REFERENCE_TEXT_FIELD));
        request.setUserid(ctPaymentCustomFields.getFieldAsString(CustomFieldKeys.USER_ID_FIELD));

        request.setSuccessurl(ctPaymentCustomFields.getFieldAsString(CustomFieldKeys.SUCCESS_URL_FIELD));
        request.setErrorurl(ctPaymentCustomFields.getFieldAsString(CustomFieldKeys.ERROR_URL_FIELD));
        request.setBackurl(ctPaymentCustomFields.getFieldAsString(CustomFieldKeys.CANCEL_URL_FIELD));
    }

    /**
     * Map planned major unit (EUR, USD) amount value and currency to minor (cents) unit
     * from {@code payment} to {@code request}.
     *
     * @param request {@link AuthorizationRequest} to which set the values
     * @param payment {@link Payment} from which read amount planned.
     */
    public static void mapAmountPlannedFromPayment(final AuthorizationRequest request, final Payment payment) {
        ofNullable(payment.getAmountPlanned())
                .ifPresent(amount -> {
                    request.setCurrency(amount.getCurrency().getCurrencyCode());
                    request.setAmount(convertMinorPart()
                            .queryFrom(amount)
                            .intValue());
                });
    }

    /**
     * Try to define gender from custom fields by {@link CustomFieldKeys#GENDER_FIELD} key.
     * <p>
     * For payone gender must be a lowercase single character.
     * </p>
     * <p>
     * <b>Note:</b> for some payment types (like Klarna) gender is mandatory.
     * </p>
     * <p>
     * In this implementation the gender lookup has the next order:<ul>
     * <li>{@link Payment#getCustom()}</li>
     * <li>{@link CartLike#getCustom()}</li>
     * <li>{@code Payment.getCustomer().}{@link Customer#getCustom() getCustom()}, if exists</li>
     * <li>Fallback to empty Optional, if not found</li>
     * </ul>
     * </p>
     *
     * @param paymentWithCartLike {@link PaymentWithCartLike} from which to lookup the gender
     * @return First available lowercase single character gender if exists.
     */
    public static Optional<String> getGenderFromPaymentCart(@Nonnull final PaymentWithCartLike paymentWithCartLike) {
        return fetchFirstAvailableGender(asList(
                paymentWithCartLike.getPayment()::getCustom,
                paymentWithCartLike.getCartLike()::getCustom,
                () -> Optional.of(paymentWithCartLike)
                        .map(PaymentWithCartLike::getPayment)
                        .map(Payment::getCustomer)
                        .map(Reference::getObj)
                        .map(Customer::getCustom)
                        .orElse(null)));
    }

    /**
     * Define localization name (ISO 639) from the {@code paymentWithCartLike} in the next order:<ul>
     * <li>if payment's custom filed <i>languageCode</i> is set - return this value</li>
     * <li>else if cartLike's {@code locale} is set - return {@link Locale#getLanguage()}</li>
     * <li>otherwise return {@link Optional#empty()}</li>
     * </ul>
     *
     * @param paymentWithCartLike payment to lookup for the locale
     * @return Optional String of 2 characters localization name by ISO 639, or {@link Optional#empty()} if not found.
     */
    public static Optional<String> getPaymentLanguage(@Nullable final PaymentWithCartLike paymentWithCartLike) {
        Optional<PaymentWithCartLike> paymentOptional = ofNullable(paymentWithCartLike);

        return paymentOptional
                .map(PaymentWithCartLike::getPayment)
                .map(Payment::getCustom)
                .map(customFields -> customFields.getFieldAsString(LANGUAGE_CODE_FIELD))
                .map(Optional::of)
                .orElseGet(() -> paymentOptional
                        .map(PaymentWithCartLike::getCartLike)
                        .map(CartLike::getLocale)
                        .map(Locale::getLanguage));
    }

    /**
     * Same as {@link #getPaymentLanguage(PaymentWithCartLike)}, but falls back to
     * {@link com.commercetools.util.ServiceConstants#DEFAULT_LOCALE} locale
     * if not found in {@code paymentWithCartLike}.
     *
     * @param paymentWithCartLike payment to lookup for the locale
     * @return String of 2 characters localization name by ISO 639
     */
    @Nonnull
    public static String getPaymentLanguageTagOrFallback(@Nullable final PaymentWithCartLike paymentWithCartLike) {
        return getPaymentLanguage(paymentWithCartLike).orElse(DEFAULT_LOCALE.getLanguage());
    }

    /**
     * Iterate the custom field suppliers and find the first one which contains a value in
     * {@link CustomFieldKeys#GENDER_FIELD}.
     *
     * @param customFieldSuppliers list of suppliers of custom fields. The suppliers expected to be non-null, but
     *                             supplied {@link Supplier#get()} result could be null, if custom field is not available for some types.
     * @return First available lowercase single character gender if exists.
     */
    @Nonnull
    private static Optional<String> fetchFirstAvailableGender(List<Supplier<? extends CustomFields>> customFieldSuppliers) {
        return customFieldSuppliers.stream()
                .map(Supplier::get)
                .map(MappingUtil::getGenderFromCustomField)
                .filter(Optional::isPresent)
                .findFirst()
                .orElseGet(Optional::empty);
    }

    /**
     * Try to fetch {@link CustomFieldKeys#GENDER_FIELD} from the {@code customFields} and map it to Payone acceptable
     * value.
     *
     * @param customFields {@link CustomFields} from which to fetch gender property.
     * @return lowercase single character gender if exists.
     */
    @Nonnull
    private static Optional<String> getGenderFromCustomField(@Nullable CustomFields customFields) {
        return ofNullable(customFields)
                .map(custom -> custom.getFieldAsString(CustomFieldKeys.GENDER_FIELD))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(gender -> gender.substring(0, 1))
                .map(String::toLowerCase);
    }

    /**
     * Convert {@link LocalDate} {@code birthday} to payone specific birthday string.
     *
     * @param birthday birthday date to convert.
     * @return "yyyyMMdd" formatted date or <b>null</b>, of {@code birthday} is <b>null</b>
     */
    @Nullable
    public static String dateToBirthdayString(@Nullable LocalDate birthday) {
        return ofNullable(birthday)
                .map(date -> date.format(yyyyMMdd))
                .orElse(null);
    }

    /**
     * From list of {@code addresses} try to read a value by {@code addressPropertyReader} and return it if exists.
     *
     * @param addresses             ordered list of addresses. The first found significant value from an address from
     *                              the list will be returned.
     * @param addressPropertyReader a {@link Function} which accepts address and reads a value from it.
     * @param <A>                   {@link Address}
     * @param <R>                   type of property to read
     * @return First found significant property value from {@code addresses}. Otherwise - {@link Optional#empty()}
     */
    public static <A extends Address, R>
    Optional<R> getFirstValueFromAddresses(@Nonnull List<A> addresses,
                                           @Nonnull Function<A, R> addressPropertyReader) {
        return addresses.stream()
                .filter(Objects::nonNull)
                .map(addressPropertyReader)
                .filter(Objects::nonNull)
                .findFirst();
    }
}
