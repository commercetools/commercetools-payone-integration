package com.commercetools.pspadapter.tenant;

import net.logstash.logback.marker.LogstashMarker;

import javax.annotation.Nonnull;

import static net.logstash.logback.marker.Markers.append;

public final class TenantLoggerUtil {

    /**
     * Create a LogStashMarker with name which consists of the tenant name.
     * @param tenantName tenant name to append to the logger.
     * @return a LogStashMarker with name which consists of the tenant name.
     */
    public static LogstashMarker createTenantKeyValue(@Nonnull final String tenantName) {
        return append("tenantName", tenantName);
    }

    private TenantLoggerUtil() {
    }
}
