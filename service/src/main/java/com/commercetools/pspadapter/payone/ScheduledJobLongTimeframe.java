package com.commercetools.pspadapter.payone;

import java.time.ZonedDateTime;

public class ScheduledJobLongTimeframe extends ScheduledJob {
    @Override
    protected ZonedDateTime getSinceDateTime() {
        return ZonedDateTime.now().minusDays(2);
    }
}
