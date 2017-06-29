package com.commercetools.pspadapter.payone;

import java.time.ZonedDateTime;

public class ScheduledJobShortTimeframe extends ScheduledJob {

    /**
     * @return 10 minutes ago.
     */
    @Override
    protected ZonedDateTime getSinceDateTime() {
        return ZonedDateTime.now().minusMinutes(10);
    }
}
