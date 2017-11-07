package com.commercetools.pspadapter.payone.util;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;


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
        return Hashing.md5().hashString(in, Charsets.UTF_8).toString();
    }
}
