package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.domain.payone.model.common.AuthorizationRequest;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;

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

        Preconditions.checkArgument(billingAddress != null, "billing address is null");

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

    public static void mapCustomerToRequest(final AuthorizationRequest request, final Reference<Customer> customer) {

        Preconditions.checkArgument(customer != null && customer.getObj() != null, "no or empty reference to customer");

        request.setVatid(customer.getObj().getVatId());

        //birthday
        Optional.ofNullable(customer.getObj().getDateOfBirth()).ifPresent(birthday -> {
            request.setBirthday(birthday.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        });

        //TODO: Gender can also be a CustomField enum @Cart with values Female/Male
        //gender
        Optional.ofNullable(customer.getObj().getCustom()).ifPresent(customs -> {
            Optional.ofNullable(customs.getFieldAsString(CustomFieldKeys.GENDER_FIELD))
                    .ifPresent(gender -> {
                        request.setGender(gender.substring(0, 0));
                    });
        });

        //customerNumber
        Optional.ofNullable(customer.getObj().getCustomerNumber())
            .ifPresent(customerNumber -> {
                if (customerNumber.length() > 20) {
                    LOG.warn("customer.customerNumber exceeds the maximum length of 20! Using substring of customer.id as fallback.");
                    String id = customer.getObj().getId();
                    id = id.replace("-", "").substring(0, 19);
                    request.setCustomerid(id);
                } else {
                    request.setCustomerid(customerNumber);
                }
            });
    }

    public static void mapShippingAddressToRequest(final AuthorizationRequest request, final Address shippingAddress) {

        Preconditions.checkArgument(shippingAddress != null, "shipping address is null");

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
}
