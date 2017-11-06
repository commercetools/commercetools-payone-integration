package com.commercetools.pspadapter.payone.util;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;


public final class PayoneHash {

    /**
     * Currently Payone doesn't support SHA2. We should change the hashing method
     * after they introduce support for SHA2
     * */
    @SuppressWarnings("deprecation")
    public static String calculate(String in) {
        return Hashing.md5().hashString(in, Charsets.UTF_8).toString();
    }
}
