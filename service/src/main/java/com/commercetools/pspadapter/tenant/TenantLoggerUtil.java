package com.commercetools.pspadapter.tenant;

import javax.annotation.Nonnull;

import static java.lang.String.format;

public final class TenantLoggerUtil {

    /**
     * Opposite to logger name generated in {@link #createLoggerName(Class, String)}
     * this one is used in static common loggers where it is not possible to generate separate tenant loggers.
     */
    public static final String LOG_PREFIX = "[tenant {}]";

    /**
     * Create a logger with name which consists of full qualified {@code clazz} name
     * and the suffix "[tenant &lt;name&gt;]".
     * @param clazz Class where the logger is used
     * @param tenantName tenant name to attach to the class name.
     * @return Concatenated FQDN and the {@code tenantName} wrapped to "[tenant &lt;name&gt;]" construction.
     */
    public static String createLoggerName(@Nonnull Class clazz, @Nonnull String tenantName) {
        return format("%s[tenant %s]", clazz.getName(), tenantName);
    }

    private TenantLoggerUtil() {
    }
}
