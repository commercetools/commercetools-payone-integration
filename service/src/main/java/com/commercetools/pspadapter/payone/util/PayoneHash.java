package com.commercetools.pspadapter.payone.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.nio.charset.StandardCharsets;


public final class PayoneHash {

    /**
     * Calculate MD5 hash sum of a given input string
     * <p>
     * <b>Note:</b> Currently Payone <b>server</b> API (opposite to client API) doesn't support SHA2.
     * We should change the hashing method as soon as they introduce support for SHA2 on server API.
     *
     * @param in from which to calculate the hash
     * @return MD5 hash string
     */
    @SuppressWarnings("deprecation")
    public static String calculate(String in) {
        return DigestUtils.md5Hex(in);
    }
}
