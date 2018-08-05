package com.faforever.client.reporting;

import com.bugsnag.Bugsnag;
import com.bugsnag.Severity;
import com.bugsnag.callbacks.Callback;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.BugsnagConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;


@RunWith(MockitoJUnitRunner.class)
public class ReportingServiceTest {
  @Mock
  private Bugsnag bugsnag;

  private ReportingService instance;

  @Before
  public void setUp() throws Exception {
    ClientProperties clientProperties = new ClientProperties();
    BugsnagConfig bugsnagConfig = new BugsnagConfig();
    bugsnagConfig.setLogLinesSent(300);
    bugsnagConfig.setToken("abc");
    clientProperties.setBugsnagConfig(bugsnagConfig);
    instance = new ReportingService(bugsnag, clientProperties);
  }

  @Test
  public void testReport() {
    Exception test = new Exception("test");
    instance.silentlyReport(test);
    Mockito.verify(bugsnag).notify(eq(test), eq(Severity.ERROR), any(Callback.class));
  }
}