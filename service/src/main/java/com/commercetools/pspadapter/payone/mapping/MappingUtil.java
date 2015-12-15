package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.domain.payone.model.common.PreauthorizationRequest;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * @author fhaertig
 * @date 13.12.15
 */
public class MappingUtil {

    private static final Logger LOG = LoggerFactory.getLogger(MappingUtil.class);

    public static void mapBillingAddressToRequest(
            final PreauthorizationRequest request,
            final Address billingAddress) {

        //required
        request.setLastname(billingAddress.getLastName());
        request.setCountry(billingAddress.getCountry().toLocale().getCountry());

        //optional
        request.setTitle(billingAddress.getTitle());
        request.setSalutation(billingAddress.getSalutation());
        request.setFirstname(billingAddress.getFirstName());
        request.setCompany(billingAddress.getCompany());
        request.setStreet(billingAddress.getStreetName() + Optional.ofNullable(billingAddress.getStreetNumber()));
        request.setAddressaddition(billingAddress.getAdditionalStreetInfo());
        request.setZip(billingAddress.getPostalCode());
        request.setCity(billingAddress.getCity());
        request.setEmail(billingAddress.getEmail());
        request.setTelephonenumber(Optional
                .ofNullable(billingAddress.getPhone())
                .orElse(billingAddress.getMobile()));

        //billingAddress.state write to PAYONE only if country=US, CA, CN, JP, MX, BR, AR, ID, TH, IN)
        // and only if value is an ISO3166-2 subdivision
    }

    public static void mapCustomerToRequest(final PreauthorizationRequest request, final Reference<Customer> customer) {

        if (customer != null && customer.getObj() != null) {
            request.setVatid(customer.getObj().getVatId());

            //birthday
            Optional.ofNullable(customer.getObj().getDateOfBirth()).ifPresent(birthday -> {
                request.setBirthday(birthday.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            });

            //TODO: Gender can also be a CustomField enum @Cart with values Female/Male
            //gender
            Optional.ofNullable(customer.getObj().getCustom()).ifPresent(customs -> {
                Optional.ofNullable(customs.getFieldAsString(CustomFieldKeys.GENDER_KEY))
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
    }

    public static void mapShippingAddressToRequest(final PreauthorizationRequest request, final Address shippingAddress) {

        //TODO: shipping data in request object

    }
}
