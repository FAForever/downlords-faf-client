package com.faforever.client.reporting;

import com.faforever.client.update.Version;
import io.sentry.Sentry;
import io.sentry.context.Context;
import io.sentry.dsn.Dsn;
import io.sentry.event.UserBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import static io.sentry.dsn.Dsn.DEFAULT_DSN;


@Lazy
@Service
@Slf4j
public class ReportingServiceImpl implements ReportingService {

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
    log.warn("Logging error to sentry: {}", e);
    Sentry.capture(e);
  }

  public static void initAutoReporting() {
    String dsn = DEFAULT_DSN;

    if (!Version.VERSION.equals(Version.SNAPSHOT)) {
      try {
        dsn = Dsn.dsnLookup();
      } catch (Exception e) {
        log.error("Error creating valid DSN from: '{}'.", dsn, e);
      }
    }

    Sentry.init(dsn);
  }
}
