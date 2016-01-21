package com.commercetools.pspadapter.payone;

import java.time.ZonedDateTime;

public class ScheduledJobShortTimeframe extends ScheduledJob {
    @Override
    protected ZonedDateTime getSinceDateTime() {
        return ZonedDateTime.now().minusMinutes(10);
    }
}
