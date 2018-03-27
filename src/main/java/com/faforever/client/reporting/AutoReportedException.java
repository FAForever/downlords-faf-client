package com.faforever.client.reporting;

import io.sentry.Sentry;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class AutoReportedException extends RuntimeException {
    public AutoReportedException(Throwable cause) {
      Sentry.capture(cause);
      LoggerFactory.getLogger((MethodHandles.lookup().lookupClass())).warn("Logging error to sentry: {}", cause.toString());
    }
}
