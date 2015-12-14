package com.commercetools.pspadapter.payone.domain.payone;

import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneBaseRequest;
import com.google.common.base.Preconditions;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PayonePostServiceImpl implements PayonePostService {

    private static Logger logger = LoggerFactory.getLogger(PayonePostServiceImpl.class);
    private static final String ENCODING_UTF8 = "UTF-8";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded;charset=UTF-8";

    private String serverAPIURL;

    private int readTimeOut = 10000;
    private int connectionTimeout = 3000;

    private PayonePostServiceImpl(final String serverAPIURL) {
        Preconditions.checkArgument(
                serverAPIURL != null && !serverAPIURL.isEmpty(),
                "The server api url must not be null or empty.");
        this.serverAPIURL = serverAPIURL;
    }

    /**
     * Initialize new service with url.
     * @param payoneServerApiUrl - the payone server api url, must not be null or empty
     * @return new instance of PayonePostServiceImpl.class
     * @throws IllegalArgumentException if the provided {@code payoneServerApiUrl} is invalid
     */
    public static PayonePostServiceImpl of(final String payoneServerApiUrl) throws IllegalArgumentException {
        return new PayonePostServiceImpl(payoneServerApiUrl);
    }

    @Override
    public Map<String, String> executePost(final PayoneBaseRequest requestParams) throws PayoneException {

        logger.info("Payone POST request parameters: " + requestParams.toStringMap().toString());
        // Now after all parameters are prepared then send those parameters into
        // the target URL using HTTPS protocol and analyze the response. Please
        // be aware that you have configured your web server to locate
        // the keystore properly. The example how to dynamically change the
        // configuration is shown as follow:
        //
        // System.setProperty("javax.net.ssl.trustStore","C:/Tomcat5.5/mykeystorefile");
        // System.setProperty("javax.net.ssl.trustStorePassword", "mypassword");
        //

        try {
            String serverResponse = Unirest.post(this.serverAPIURL)
                    .fields(requestParams.toStringMap())
                    .asString().getBody();

            logger.info("Payone POST response: " + serverResponse);
        } catch (UnirestException e) {
            throw new PayoneException("Payone POST request failed.", e);
        }

        return null;
    }

    Map<String, String> buildMapFromResultParams(final List<String> serverResponse) throws UnsupportedEncodingException {
        Map<String, String> resultMap = new HashMap<String, String>();
        for (String listElement : serverResponse) {
            String[] param = StringUtils.split(listElement, "=", 2);
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

    StringBuffer buildRequestParamsString(final Map<String, String> requestParams) throws PayoneException {
        StringBuffer params = new StringBuffer();

        Iterator<String> it = requestParams.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            String value = requestParams.get(key);
            try {
                params.append(URLEncoder.encode(key, ENCODING_UTF8) + "=" + URLEncoder.encode(value, ENCODING_UTF8));
            } catch (UnsupportedEncodingException e) {
                throw new PayoneException("Payone POST request failed. Error while encoding values with UTF8", e);
            }

            if (it.hasNext()) {
                params.append("&");
            }
        }


        return params;
    }

    public String getServerAPIURL() {
        return serverAPIURL;
    }

    public void setServerAPIURL(String serverAPIURL) {
        this.serverAPIURL = serverAPIURL;
    }

    public int getReadTimeOut() {
        return readTimeOut;
    }

    public void setReadTimeOut(int readTimeOut) {
        this.readTimeOut = readTimeOut;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

}