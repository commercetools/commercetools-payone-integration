package com.commercetools.pspadapter.payone.notification;

import com.commercetools.payments.TransactionStateResolver;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.commercetools.pspadapter.payone.mapping.order.PaymentToOrderStateMapper;
import com.commercetools.pspadapter.tenant.TenantConfig;
import com.commercetools.pspadapter.tenant.TenantFactory;
import com.commercetools.service.OrderService;
import com.commercetools.service.PaymentService;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.commands.updateactions.SetStatusInterfaceCode;
import io.sphere.sdk.payments.commands.updateactions.SetStatusInterfaceText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.commercetools.pspadapter.payone.util.CompletionUtil.executeBlocking;
import static com.commercetools.pspadapter.tenant.TenantLoggerUtil.createLoggerName;

/**
 * Base for notification processor implementations.
 *
 * @author Jan Wolter
 */
public abstract class NotificationProcessorBase implements NotificationProcessor {
    private static final String DEFAULT_SEQUENCE_NUMBER = "0";

    private final Logger logger;

    private final TenantFactory tenantFactory;

    private final TenantConfig tenantConfig;

    private final TransactionStateResolver transactionStateResolver;

    /**
     * Constructor for implementations.
     *
     * @param tenantFactory            the tenant services factory for commercetools platform API
     * @param tenantConfig             the current tenant config, for which notification is received
     * @param transactionStateResolver helper which defines if a transaction state is assumed as "not completed"
     */
    protected NotificationProcessorBase(final TenantFactory tenantFactory, final TenantConfig tenantConfig,
                                        final TransactionStateResolver transactionStateResolver) {
        this.tenantFactory = tenantFactory;
        this.tenantConfig = tenantConfig;
        this.transactionStateResolver = transactionStateResolver;

        this.logger = LoggerFactory.getLogger(createLoggerName(this.getClass(), tenantConfig.getName()));
    }

    @Override
    public final void processTransactionStatusNotification(final Notification notification, final Payment payment) {
        if (!canProcess(notification)) {
            throw new IllegalArgumentException(String.format(
                    "txaction \"%s\" is not supported by %s",
                    notification.getTxaction(),
                    this.getClass().getName()));
        }

        try {
            // 1. update payment
            CompletionStage<Order> fullCompletionStage = getPaymentService().updatePayment(payment, createPaymentUpdates(payment, notification))
                    // 2. try to find and updated respective Order.paymentState if required
                    .thenComposeAsync(this::tryToUpdateOrderByPayment);

            executeBlocking(fullCompletionStage);

        } catch (final io.sphere.sdk.client.ConcurrentModificationException e) {
            throw new java.util.ConcurrentModificationException(e);
        }
    }

    /**
     * If {@link #tenantConfig#isUpdateOrderPaymentState()} is <b>true</b> - fetch an order with respective payment id
     * and try to update that order. If <b>false</b> - return completed stage with <b>null</b> value.
     *
     * @param updatedPayment <b>non-null</b> new updated payment instance.
     * @return completion stage with nullable updated {@link Order} if was updated.
     */
    private CompletionStage<Order> tryToUpdateOrderByPayment(Payment updatedPayment) {
        if (tenantConfig.isUpdateOrderPaymentState()) {
            return getOrderService().getOrderByPaymentId(updatedPayment.getId())
                    .thenComposeAsync(order -> updateOrderIfExists(order.orElse(null), updatedPayment));
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * If the {@code order} is not null and it's payment status is different from the new one - update it,
     * otherwise - skip updating.
     * <p>
     * If a new payment status can't be mapped to the order's payment state - the value is left unchanged.
     *
     * @param order          <b>nullable</b> order to update state. If <b>null</b> - the update is skipped,
     *                       but the incident is reported.
     * @param updatedPayment <b>non-null</b> instance of the updated payment
     * @return a {@link CompletionStage} with <b>nullable</b> updated {@link Order} instance. If incoming order is null
     * or new payment state is undefined, or update is not required - the returned stage is completed and contains
     * the same instance.
     */
    private CompletionStage<Order> updateOrderIfExists(Order order, Payment updatedPayment) {
        if (order != null) {
            PaymentState newPaymentState = getPaymentToOrderStateMapper().mapPaymentToOrderState(updatedPayment);

            if (newPaymentState == null && updatedPayment.getPaymentStatus() != null) {
                logger.warn("Payment [{}] has paymentStatus [{}] which can't be mapped to Order#paymentState. "
                                + "The order's state remains unchanged.",
                        updatedPayment.getId(), updatedPayment.getPaymentStatus().getInterfaceCode());
            }

            // skip update for undefined or unchanged payment state
            if (newPaymentState != null && !newPaymentState.equals(order.getPaymentState())) {
                return getOrderService().updateOrderPaymentState(order, newPaymentState);
            }
        } else {
            // This may happen when Payone sends the notification faster then user was redirected back to the shop
            // and the order has not been created yet.
            logger.info("Payment [{}] is updated, but respective order is not found!", updatedPayment.getId());
        }

        return CompletableFuture.completedFuture(order);
    }

    /**
     * Returns whether the provided PAYONE {@code notification} can be processed by this instance.
     *
     * @param notification a PAYONE notification
     * @return whether the {@code notification} can be processed
     */
    protected abstract boolean canProcess(Notification notification);

    /**
     * @see TransactionStateResolver#isNotCompletedTransaction(io.sphere.sdk.payments.Transaction)
     */
    protected boolean isNotCompletedTransaction(@Nonnull Transaction transaction) {
        return transactionStateResolver.isNotCompletedTransaction(transaction);
    }

    /**
     * Generates a list of update actions which can be applied to the payment in one step.
     *
     * @param payment      the payment to which the updates may apply
     * @param notification the notification to process
     * @return an immutable list of update actions which will e.g. add an interfaceInteraction to the payment
     * or apply changes to a corresponding transaction in the payment
     */
    protected ImmutableList<UpdateAction<Payment>> createPaymentUpdates(final Payment payment,
                                                                        final Notification notification) {
        final ImmutableList.Builder<UpdateAction<Payment>> listBuilder = ImmutableList.builder();
        listBuilder.add(createNotificationAddAction(notification));
        listBuilder.add(setStatusInterfaceCode(notification));
        listBuilder.add(setStatusInterfaceText(notification));
        return listBuilder.build();
    }

    /**
     * Creates a payment update action to add the given notification as
     * {@value CustomTypeBuilder#PAYONE_INTERACTION_NOTIFICATION}.
     *
     * @param notification the PAYONE notification
     * @return the update action
     */
    protected static AddInterfaceInteraction createNotificationAddAction(final Notification notification) {
        return AddInterfaceInteraction.ofTypeKeyAndObjects(
                CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                ImmutableMap.of(
                        CustomFieldKeys.TIMESTAMP_FIELD, toZonedDateTime(notification),
                        CustomFieldKeys.SEQUENCE_NUMBER_FIELD, toSequenceNumber(notification.getSequencenumber()),
                        CustomFieldKeys.TX_ACTION_FIELD, notification.getTxaction().getTxActionCode(),
                        CustomFieldKeys.NOTIFICATION_FIELD, notification.toString()));
    }

    /**
     * Creates a payment update action to set the latest txaction to StatusInterfaceCode
     * @param notification the PAYONE notification
     * @return the update action
     */
    protected static SetStatusInterfaceCode setStatusInterfaceCode(final Notification notification) {
        return SetStatusInterfaceCode.of(notification.getTxaction().toString());
    }

    /**
     * Creates a payment update action to set the latest txaction to StatusInterfaceText
     * @param notification the PAYONE notification
     * @return the update action
     */
    protected static SetStatusInterfaceText setStatusInterfaceText(final Notification notification) {
        return SetStatusInterfaceText.of(notification.getTxaction().toString());
    }

    /**
     * Converts the notification's "txtime" to a zoned datetime.
     * @param notification the notification to get the transaction timestamp from
     * @return the transaction timestamp
     */
    protected static ZonedDateTime toZonedDateTime(final Notification notification) {
        return ZonedDateTime.of(
                LocalDateTime.ofEpochSecond(Long.valueOf(notification.getTxtime()), 0, ZoneOffset.UTC),
                ZoneId.of("UTC"));
    }

    /**
     * Checks whether {@code transactions} contains a transaction of the given {@code transactionType} with the
     * given {@code interactionId}.
     *
     * @param transactions    the list of transactions
     * @param transactionType the type of transaction
     * @param interactionId   the interaction ID (aka PAYONE's sequencenumber)
     * @return whether there is a {@code CHARGE} transaction with the provided {@code interactionId} in
     * the {@code transactions}
     */
    protected static Optional<Transaction> findMatchingTransaction(final Collection<Transaction> transactions,
                                                                   final TransactionType transactionType,
                                                                   final String interactionId) {
        return transactions.stream()
                .filter(transaction -> transaction.getType().equals(transactionType)
                        && toSequenceNumber(transaction.getInteractionId()).equals(interactionId))
                .findFirst();
    }

    /**
     * Transforms the {@code sequenceNumber} into {@value #DEFAULT_SEQUENCE_NUMBER} if necessary.
     *
     * @param sequenceNumber the sequence number string
     * @return the {@code sequenceNumber} if not null, {@value #DEFAULT_SEQUENCE_NUMBER} otherwise
     */
    @Nonnull
    protected static String toSequenceNumber(final String sequenceNumber) {
        return MoreObjects.firstNonNull(sequenceNumber, DEFAULT_SEQUENCE_NUMBER);
    }

    protected final PaymentService getPaymentService() {
        return tenantFactory.getPaymentService();
    }

    protected final OrderService getOrderService() {
        return tenantFactory.getOrderService();
    }

    protected final PaymentToOrderStateMapper getPaymentToOrderStateMapper() {
        return tenantFactory.getPaymentToOrderStateMapper();
    }
}
