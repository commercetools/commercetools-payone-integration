package com.commercetools.pspadapter.payone.domain.payone;

import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneConfigurationException;
import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;

import java.util.Map;

/**
 * Encapsulates the communication with the payone services.
 *
 */
public interface PayonePostService {

    /**
     * Setup connection and execute post request with params.
     * @param requestParams set to request
     * @return Map containing the servers response
     * @throws PayoneException - the payone exception
     */
    Map<String, String> executePost(final Map<String, String> requestParams) throws PayoneException;

    /**
     * Initialize new service with url.
     * @param payoneServerApiUrl - the payone server api url
     * @return new instance of PayonePostServiceImpl.class
     * @throws PayoneConfigurationException - the payone configuration exception
     */
    static PayonePostService of(final String payoneServerApiUrl) throws PayoneConfigurationException {
        return new PayonePostServiceImpl(payoneServerApiUrl);
    }

}