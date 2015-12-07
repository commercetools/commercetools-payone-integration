package specs.common;

import com.commercetools.pspadapter.payone.IntegrationService;
import com.commercetools.pspadapter.payone.ScheduledJobFactory;
import com.commercetools.pspadapter.payone.ServiceConfig;
import com.commercetools.pspadapter.payone.ServiceFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentDraft;
import io.sphere.sdk.payments.PaymentDraftBuilder;
import io.sphere.sdk.payments.TransactionDraft;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.commands.PaymentDeleteCommand;
import io.sphere.sdk.utils.MoneyImpl;
import org.concordion.api.ExpectedToFail;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;

import javax.money.MonetaryAmount;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/**
 * @author fhaertig
 * @date 04.12.15
 */
@ExpectedToFail
@RunWith(ConcordionRunner.class)
public class ServiceSchedulingFixture {

    private static final String SCHEDULED_JOB_KEY = "commercetools-polljob-1";

    private SphereClient client;
    private Payment p;
    private Scheduler scheduler;
    private IntegrationService integrationService;

    @Before
    public void setUp() throws ExecutionException, InterruptedException, SchedulerException {
        String ctProjectKey = System.getProperty("CT_PROJECT_KEY");
        String ctClientId = System.getProperty( "CT_CLIENT_ID" );
        String ctClientSecret = System.getProperty( "CT_CLIENT_SECRET" );

        SphereClientFactory factory = SphereClientFactory.of();
        client = factory.createClient(ctProjectKey, ctClientId, ctClientSecret);

        MonetaryAmount monetaryAmount = MoneyImpl.ofCents(20000, "EUR");

        final List<TransactionDraft> transactions = Collections.singletonList(TransactionDraftBuilder
                .of(TransactionType.CHARGE, monetaryAmount, ZonedDateTime.now())
                .timestamp(ZonedDateTime.now())
                .interactionId("1")
                .state(TransactionState.PENDING)
                .build());

        PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .transactions(transactions).build();

        PaymentCreateCommand paymentCreateCommand = PaymentCreateCommand.of(paymentDraft);
        final CompletionStage<Payment> payment = client.execute(paymentCreateCommand);
        p = payment.toCompletableFuture().get();

        integrationService = ServiceFactory.createService(new ServiceConfig());
    }

    @After
    public void tearDown() {
        client.execute(PaymentDeleteCommand.of(p));
    }

    public JobResult checkJobScheduling(String cronNotation) throws SchedulerException, InterruptedException {
        scheduler = ScheduledJobFactory.createScheduledJob(
                CronScheduleBuilder.cronSchedule(cronNotation),
                integrationService,
                SCHEDULED_JOB_KEY);
        TriggerKey triggerKey = new TriggerKey(SCHEDULED_JOB_KEY);

        JobResult result = new JobResult(scheduler.getTriggerState(triggerKey).name(),
                            ((CronTrigger)scheduler.getTrigger(triggerKey)).getCronExpression());
        Thread.sleep(2000);
        scheduler.shutdown();
        return result;
    }

    public HttpResult sendHandleRequest() {
        //TODO: send request with id of dummy payment.
        return new HttpResult("TODO", "TODO");
    }

    class JobResult {
        public String triggerState;
        private String triggerCronExpression;

        public JobResult( final String triggerState, final String triggerCronExpression) {
            this.triggerState = triggerState;
            this.triggerCronExpression = triggerCronExpression;
        }

        public boolean validateCronNotation(final String cronNotation) {
            return this.triggerCronExpression.equals(cronNotation);
        }

        public boolean isTriggerStateNormal() {
            return this.triggerState.equals("NORMAL");
        }
    }

    class HttpResult {
        public String header;
        public String body;

        public HttpResult(final String header, final String body) {
            this.header = header;
            this.body = body;
        }

        public boolean hasEmptyBody() {
            return this.body.isEmpty();
        }
    }
}
