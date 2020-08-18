package com.commercetools.util;

import io.sphere.sdk.http.HttpHeaders;
import org.slf4j.MDC;
import spark.Request;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

import static net.logstash.logback.encoder.org.apache.commons.lang3.StringUtils.isBlank;

public final class CorrelationIdUtil {
    public static final String CORRELATION_ID_LOG_VAR_NAME = "correlationId";

    public static void attachFromRequestOrGenerateNew(@Nonnull final Request request) {
        MDC.put(CORRELATION_ID_LOG_VAR_NAME, getOrGenerate(request.headers(HttpHeaders.X_CORRELATION_ID)));
    }

    public static String getFromMDCOrGenerateNew() {
        final String correlationId = getOrGenerate(MDC.get(CORRELATION_ID_LOG_VAR_NAME));
        MDC.put(CORRELATION_ID_LOG_VAR_NAME, correlationId);
        return correlationId;
    }

    private static String getOrGenerate(@Nullable final String correlationId) {
        return isBlank(correlationId)? UUID.randomUUID().toString() : correlationId;
    }

    private CorrelationIdUtil() {
    }
}
