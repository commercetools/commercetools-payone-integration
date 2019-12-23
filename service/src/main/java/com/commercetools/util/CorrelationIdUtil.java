package com.commercetools.util;

import io.sphere.sdk.http.HttpHeaders;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import spark.Request;

import javax.annotation.Nonnull;
import java.util.UUID;

import static java.util.Optional.ofNullable;

public final class CorrelationIdUtil {
    public static final String CORRELATION_ID_LOG_VAR_NAME = "correlationId";

    public static String getOrGenerate(@Nonnull final Request request) {
        return generateIfBlank(request.headers(HttpHeaders.X_CORRELATION_ID));
    }

    private static String generateIfBlank(@Nonnull final String headerValue) {
        if (StringUtils.isBlank(headerValue)) {
            return getFromMDCOrGenerateNew();
        }
        return headerValue;
    }

    private static String getFromMDCOrGenerateNew() {
        return ofNullable(MDC.get(CORRELATION_ID_LOG_VAR_NAME))
            .orElseGet(CorrelationIdUtil::generateUniqueCorrelationId);
    }

    public static String generateUniqueCorrelationId() {
        return UUID.randomUUID().toString();
    }

    private CorrelationIdUtil() {
    }
}
