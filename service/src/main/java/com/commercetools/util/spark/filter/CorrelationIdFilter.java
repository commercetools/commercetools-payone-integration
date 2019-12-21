package com.commercetools.util.spark.filter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import spark.Filter;
import spark.Request;
import spark.Response;

import javax.annotation.Nonnull;
import java.util.UUID;

public class CorrelationIdFilter implements Filter {
    private static final String CORRELATION_ID_HEADER_NAME = "X-Correlation-Id";
    private static final String CORRELATION_ID_LOG_VAR_NAME = "correlationId";

    public static CorrelationIdFilter of() {
        return new CorrelationIdFilter();
    }

    @Override
    public void handle(@Nonnull final Request request, @Nonnull final Response response) {
        MDC.put(CORRELATION_ID_LOG_VAR_NAME, getCorrelationIdFromHeaderOrGenerateNew(request));
    }

    private static String getCorrelationIdFromHeaderOrGenerateNew(@Nonnull final Request request) {
        String correlationIdHeaderValue = request.headers(CORRELATION_ID_HEADER_NAME);
        if (StringUtils.isBlank(correlationIdHeaderValue)) {
            correlationIdHeaderValue = generateUniqueCorrelationId();
        }
        return correlationIdHeaderValue;
    }

    private static String generateUniqueCorrelationId() {
        return UUID.randomUUID().toString();
    }

    private CorrelationIdFilter() {
    }
}
