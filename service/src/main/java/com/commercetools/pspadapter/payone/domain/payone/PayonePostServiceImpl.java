package com.commercetools.pspadapter.payone.domain.payone;

import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
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

    public PayonePostServiceImpl(final String serverAPIURL) {
        Preconditions.checkArgument(serverAPIURL != null && !serverAPIURL.isEmpty(), "The server api url must not be null on init.");
        this.serverAPIURL = serverAPIURL;
    }

    @Override
    public Map<String, String> executePost(final Map<String, String> requestParams) throws PayoneException {

        Map<String, String> resultMap = Collections.emptyMap();

        StringBuffer params = buildRequestParamsString(requestParams);

        // Now after all parameters are prepared then send those parameters into
        // the target URL using HTTPS protocol and analyze the response. Please
        // be aware that you have configured your web server to locate
        // the keystore properly. The example how to dynamically change the
        // configuration is shown as follow:
        //
        // System.setProperty("javax.net.ssl.trustStore","C:/Tomcat5.5/mykeystorefile");
        // System.setProperty("javax.net.ssl.trustStorePassword", "mypassword");
        //

        InputStream inStream = null;
        OutputStream outStream = null;
        try {
            // Get the connection to the server
            URL locator = new URL(this.serverAPIURL);
            HttpsURLConnection conn = (HttpsURLConnection) locator.openConnection();
            conn.setConnectTimeout(this.connectionTimeout);
            conn.setReadTimeout(this.readTimeOut);
            conn.setRequestProperty("Content-type", CONTENT_TYPE);
            conn.setRequestProperty("Content-length", String.valueOf(params.length()));
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            // Write the input parameters
            outStream = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream, ENCODING_UTF8));
            writer.write(params.toString());
            writer.flush();

            // Get the response from the server
            inStream = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inStream, ENCODING_UTF8));
            String tempLine;
            List<String> serverResponse = new ArrayList<String>();
            while (StringUtils.isNotBlank(tempLine = br.readLine())) {
                serverResponse.add(tempLine);
            }
            logger.info("Payone POST response: " + serverResponse);

            // Transform the response from the server into a map so it's
            // easier to lookup necessary value.
            resultMap = buildMapFromResultParams(serverResponse);
        } catch (Exception e) {
            throw new PayoneException("Payone POST request failed.", e);
        } finally {
            // Close the input and output stream if it's open
            try {
                if (inStream != null) {
                    inStream.close();
                }
                if (outStream != null) {
                    outStream.close();
                }
            } catch (IOException e) {
                throw new PayoneException("Payone close connection streams failed.", e);
            }
        }
        return resultMap;
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

        logger.info("Payone POST request parameters: " + params.toString());
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