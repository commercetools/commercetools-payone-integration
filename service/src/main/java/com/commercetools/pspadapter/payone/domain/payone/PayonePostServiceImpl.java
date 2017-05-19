package com.commercetools.pspadapter.payone.domain.payone;

import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.commercetools.pspadapter.payone.domain.payone.model.common.BaseRequest;
import com.google.common.base.Preconditions;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PayonePostServiceImpl implements PayonePostService {

    private static final Logger LOG = LoggerFactory.getLogger(PayonePostServiceImpl.class);
    private static final String ENCODING_UTF8 = "UTF-8";

    private String serverAPIURL;

    private int connectionTimeout = 3000;


    private PayonePostServiceImpl(final String serverAPIURL) {
        Preconditions.checkArgument(
                serverAPIURL != null && !serverAPIURL.isEmpty(),
                "The server api url must not be null or empty.");
        this.serverAPIURL = serverAPIURL;
    }

    /**
     * Initialize new service with url.
     *
     * @param payoneServerApiUrl - the payone server api url, must not be null or empty
     * @return new instance of PayonePostServiceImpl.class
     * @throws IllegalArgumentException if the provided {@code payoneServerApiUrl} is invalid
     */
    public static PayonePostServiceImpl of(final String payoneServerApiUrl) throws IllegalArgumentException {
        return new PayonePostServiceImpl(payoneServerApiUrl);
    }

    @Override
    public Map<String, String> executePost(final BaseRequest baseRequest) throws PayoneException {

        try {
            Map<String, Object> mappedListParameters = getObjectMapWithExpandedLists(baseRequest.toStringMap(false));

            String serverResponse = Unirest.post(this.serverAPIURL)
                    .fields(mappedListParameters)
                    .asString().getBody();
            if (serverResponse.contains("status=ERROR")) {
                LOG.error("-> Payone POST request parameters: "
                        + getObjectMapWithExpandedLists(baseRequest.toStringMap(true)).toString());
                LOG.error("Payone POST response: " + serverResponse);
            }
            return buildMapFromResultParams(serverResponse);
        } catch (UnirestException | UnsupportedEncodingException e) {
            throw new PayoneException("Payone POST request failed. Cause: " + e.getMessage(), e);
        }
    }

    /**
     * Expand entries with list values to key-value pairs where the keys are transformed to set of {@code key[i]} with
     * respective values from the list. Non-list arguments remain the same.
     * <p>
     * The indices origin is <b>1</b>.
     * <p>
     * <b>Note:</b> so far only {@link List} values are expanded, arrays behavior is still undefined.
     * <p>
     * Example:
     * <pre> {"a": [5, 7], "b": "c", "d": ["e", "f"]} =>
     * {"a[1]": 5, "a[2]": 7, "b": "c", "d[1]": "e", "d[2]": "f"}
     * </pre>
     *
     * @param parameters Set of key-value pairs to expand
     * @return new {@link Map} where keys with list values
     */
    @Nonnull
    private Map<String, Object> getObjectMapWithExpandedLists(Map<String, Object> parameters) {
        return parameters.entrySet().stream()
                .map(entry -> {
                    Object value = entry.getValue();
                    if (value instanceof List) {
                        List list = (List) value;
                        if (list.size() > 0) {
                            for (int i = 0; i < list.size(); i++) {
                                return Pair.of(entry.getKey() + "[" + (i + 1) + "]", list.get(i));
                            }
                        } else {
                            return Pair.of(entry.getKey() + "[]", "");
                        }
                    }

                    return Pair.of(entry.getKey(), value);

                }).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    Map<String, String> buildMapFromResultParams(final String serverResponse) throws UnsupportedEncodingException {
        Map<String, String> resultMap = new HashMap<String, String>();

        String[] properties = serverResponse.split("\\r?\\n");
        for (String property : properties) {
            String[] param = StringUtils.split(property, "=", 2);
            if (param != null && param.length > 1) {
                // Attention: the Redirect-param should not be decoded, otherwise the redirect will not work
                resultMap.put(
                        param[0],
                        StringUtils.equalsIgnoreCase("redirecturl", param[0]) ? param[1] : URLDecoder.decode(param[1],
                                ENCODING_UTF8));
            }
        }
        return resultMap;
    }

    public String getServerAPIURL() {
        return serverAPIURL;
    }

    public void setServerAPIURL(String serverAPIURL) {
        this.serverAPIURL = serverAPIURL;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

}
