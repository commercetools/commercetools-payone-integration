package com.commercetools.pspadapter.payone.config;

import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.orders.Order;

/**
 * Provides the configuration of the integration service.
 *
 * @author fhaertig
 * @author Jan Wolter
 * @since 02.12.15
 */
public class ServiceConfig {

    private final SphereClientConfig sphereClientConfig;

    private final PayoneConfig payoneConfig;

    private final boolean startFromScratch;
    private final String scheduledJobCronShortTimeFrame;
    private final String scheduledJobCronLongTimeFrame;
    private final String secureKey;
    private final boolean updateOrderPaymentState;

    /**
     * Initializes the configuration.
     *
     * @param propertyProvider to get the parameters from
     * @throws IllegalStateException if a mandatory parameter is undefined or empty
     */
    public ServiceConfig(final PropertyProvider propertyProvider, final PayoneConfig payoneConfig) {

        this.sphereClientConfig = SphereClientConfig.of(
                propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.CT_PROJECT_KEY),
                propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.CT_CLIENT_ID),
                propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.CT_CLIENT_SECRET)
        );

        this.payoneConfig = payoneConfig;

        this.scheduledJobCronShortTimeFrame =
                propertyProvider.getProperty(PropertyProvider.SHORT_TIME_FRAME_SCHEDULED_JOB_CRON)
                        .map(String::valueOf)
                        .orElse("0/30 * * * * ? *");
        this.scheduledJobCronLongTimeFrame =
                propertyProvider.getProperty(PropertyProvider.LONG_TIME_FRAME_SCHEDULED_JOB_CRON)
                        .map(String::valueOf)
                        .orElse("5 0 0/1 * * ? *");
        this.startFromScratch = propertyProvider.getProperty(PropertyProvider.CT_START_FROM_SCRATCH)
                .map(Boolean::valueOf)
                .orElse(false);

        this.secureKey = propertyProvider.getProperty(PropertyProvider.SECURE_KEY)
                .map(String::valueOf)
                .orElse("");

        updateOrderPaymentState = propertyProvider.getProperty(PropertyProvider.UPDATE_ORDER_PAYMENT_STATE)
                .map(String::trim)
                .map(Boolean::valueOf)
                .orElse(false);
    }

    public SphereClientConfig getSphereClientConfig() {
        return sphereClientConfig;
    }

    public PayoneConfig getPayoneConfig() {
        return payoneConfig;
    }

    /**
     * Gets the
     * <a href="http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger">QUARTZ cron expression</a>
     * for polling commercetools messages with a shorter time frame.
     *
     * @return the cron expression
     */
    public String getScheduledJobCronForShortTimeFramePoll() {
        return scheduledJobCronShortTimeFrame;
    }

    /**
     * Gets the
     * <a href="http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger">QUARTZ cron expression</a>
     * for polling commercetools messages with a longer time frame.
     *
     * @return the cron expression
     */
    public String getScheduledJobCronForLongTimeFramePoll() {
        return scheduledJobCronLongTimeFrame;
    }

    /**
     * Gets the flag indicating whether the service shall reset the commerctools project at start up.
     *
     * @return whether the commercetools project shall be reset
     */
    public boolean getStartFromScratch() {
        return startFromScratch;
    }

    /**
     * Gets the secure key which was used for encrypting data with Blowfish.
     *
     * @return the secure key as plain text
     */
    public String getSecureKey() {
        return secureKey;
    }

    /**
     * If <b>true</b> - when processing Payone notification (update payment status) {@link Order#getPaymentState()},
     * linked to the payment, should be update also. Otherwise leave the order payment state unchanged.
     * <p>
     * By default it is <b>false</b>
     *
     * @return <b>true</b> if environment the property {@link PropertyProvider#UPDATE_ORDER_PAYMENT_STATE} is a string
     * <i>true</i> case insensitive, <b>false</b> otherwise.
     */
    public boolean isUpdateOrderPaymentState() {
        return updateOrderPaymentState;
    }
}
