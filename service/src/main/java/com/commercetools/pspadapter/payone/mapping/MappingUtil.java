package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.types.CustomFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.LANGUAGE_CODE_FIELD;

/**
 * @author fhaertig
 * @since 13.12.15
 */
public class MappingUtil {

    private static final Logger LOG = LoggerFactory.getLogger(MappingUtil.class);

    private static final Set<CountryCode> countriesWithStateAllowed = ImmutableSet.of(
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
    );

    public static void mapBillingAddressToRequest(
            final AuthorizationRequest request,
            final Address billingAddress) {

        Preconditions.checkArgument(billingAddress != null, "Missing billing address details");

        //required
        request.setLastname(billingAddress.getLastName());
        request.setCountry(billingAddress.getCountry().toLocale().getCountry());

        //optional
        request.setTitle(billingAddress.getTitle());
        request.setSalutation(billingAddress.getSalutation());
        request.setFirstname(billingAddress.getFirstName());
        request.setCompany(billingAddress.getCompany());
        request.setStreet(Joiner.on(" ")
                .skipNulls()
                .join(billingAddress.getStreetName(), billingAddress.getStreetNumber()));
        request.setAddressaddition(billingAddress.getAdditionalStreetInfo());
        request.setZip(billingAddress.getPostalCode());
        request.setCity(billingAddress.getCity());
        request.setEmail(billingAddress.getEmail());
        request.setTelephonenumber(Optional
                .ofNullable(billingAddress.getPhone())
                .orElse(billingAddress.getMobile()));

        if (countriesWithStateAllowed.contains(billingAddress.getCountry())) {
            request.setState(billingAddress.getState());
        }
    }

    public static void mapCustomerToRequest(final AuthorizationRequest request, final Reference<Customer> customerReference) {

        Preconditions.checkArgument(customerReference != null && customerReference.getObj() != null, "Missing customer object");
        final Customer customer = customerReference.getObj();

        request.setVatid(customer.getVatId());

        //birthday
        Optional.ofNullable(customer.getDateOfBirth()).ifPresent(birthday -> {
            request.setBirthday(birthday.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        });

        //TODO: Gender can also be a CustomField enum @Cart with values Female/Male
        //gender
        Optional.ofNullable(customer.getCustom()).ifPresent(customs -> {
            Optional.ofNullable(customs.getFieldAsString(CustomFieldKeys.GENDER_FIELD))
                    .ifPresent(gender -> {
                        request.setGender(gender.substring(0, 0));
                    });
        });

        //customerNumber
        Optional.ofNullable(customer.getCustomerNumber())
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

        Preconditions.checkArgument(shippingAddress != null, "Missing shipping address details");

        request.setShipping_firstname(shippingAddress.getFirstName());
        request.setShipping_lastname(shippingAddress.getLastName());
        request.setShipping_street(Joiner.on(" ")
                .skipNulls()
                .join(shippingAddress.getStreetName(), shippingAddress.getStreetNumber()));
        request.setShipping_zip(shippingAddress.getPostalCode());
        request.setShipping_city(shippingAddress.getCity());
        request.setShipping_country(shippingAddress.getCountry().toLocale().getCountry());
        request.setShipping_company(Joiner.on(" ")
                .skipNulls()
                .join(shippingAddress.getCompany(), shippingAddress.getDepartment()));

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

    public static void mapMiscellaneousFromPayment(@Nonnull final AuthorizationRequest request,
                                                   @Nonnull final PaymentWithCartLike paymentWithCartLike) {
        //customer's locale
        getPaymentLanguage(paymentWithCartLike).ifPresent(request::setLanguage);
    }

    /**
     * Define localization name (ISO 639) from the {@code paymentWithCartLike} in the next order:<ul>
     *     <li>if payment's custom filed <i>languageCode</i> is set - return this value</li>
     *     <li>else if cartLike's {@code locale} is set - return {@link Locale#getLanguage()}</li>
     *     <li>otherwise return {@link Optional#empty()}</li>
     * </ul>
     *
     * @param paymentWithCartLike payment to lookup for locale
     * @return Optional String of 2 characters localization name by ISO 639, or {@link Optional#empty()} if not found.
     */
    public static Optional<String> getPaymentLanguage(@Nullable final PaymentWithCartLike paymentWithCartLike) {
        Optional<PaymentWithCartLike> paymentOptional = Optional.ofNullable(paymentWithCartLike);

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
}
