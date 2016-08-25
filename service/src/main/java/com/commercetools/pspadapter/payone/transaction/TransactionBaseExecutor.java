package com.commercetools.pspadapter.payone.transaction;

import com.google.common.cache.LoadingCache;
import io.sphere.sdk.payments.commands.updateactions.SetStatusInterfaceCode;
import io.sphere.sdk.types.Type;

import java.util.Map;

/**
 * @author mht@dotsource.de
 * Common base class responsible for default paymentupdateactions
 */
public abstract class TransactionBaseExecutor extends IdempotentTransactionExecutor{

    private static String STATUS = "status";
    private static String ERROR_CODE = "errorcode";
    private static String ERROR = "ERROR";

    public TransactionBaseExecutor(LoadingCache<String, Type> typeCache) {
        super(typeCache);
    }

    /**
     * Creates the SetStatusInterfaceCode from the response
     * @param response contains all key that creates the
     * @return the UpdateAction that
     */
    protected SetStatusInterfaceCode setStatusInterfaceCode(final Map<String, String> response){
        final String status = response.get(STATUS);
        final StringBuilder stringBuilder = new StringBuilder(status);
        if(ERROR.equals(status)) {
            //Payone errorcode is required for error case
            stringBuilder.append(response.get(ERROR_CODE));
        }
        return SetStatusInterfaceCode.of(stringBuilder.toString());
    }
}
