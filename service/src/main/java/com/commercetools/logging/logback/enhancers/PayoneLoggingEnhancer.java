package com.commercetools.logging.logback.enhancers;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.LoggingEnhancer;

import javax.annotation.Nonnull;

public class PayoneLoggingEnhancer implements LoggingEnhancer {
    @Override
    public void enhanceLogEntry(@Nonnull final LogEntry.Builder logEntry) {
        logEntry.addLabel("test-label-1", "test-value-1");
    }
}
