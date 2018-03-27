package com.faforever.client.reporting;

import com.github.nocatch.NoCatch;
import io.sentry.Sentry;
import io.sentry.context.Context;
import io.sentry.event.UserBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;


@Lazy
@Service
public class ReportingServiceImpl implements ReportingService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void setAutoReportingUser(String username, int userId) {
    Context context = Sentry.getContext();
    UserBuilder userBuilder = new UserBuilder()
        .setUsername(username)
        .setId(String.valueOf(userId));
    context.setUser(userBuilder.build());
  }

  @Override
  public void reportError(Throwable e) {
    Sentry.capture(e);
    LoggerFactory.getLogger((MethodHandles.lookup().lookupClass())).warn("Logging error to sentry: {}", e.toString());
  }

  public static void initAutoReporting() {
    NoCatch.setDefaultWrapperException(AutoReportedException.class);
  }
}
