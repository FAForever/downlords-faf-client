package com.faforever.client.reporting;

import com.bugsnag.Bugsnag;
import com.bugsnag.Severity;
import com.faforever.client.config.ClientProperties;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
@Service
public class ReportingService {
  private final Bugsnag bugsnag;
  private final int defaultsentLogLines;
  private ReportingDialogListener reportDialogListener;

  public ReportingService(Bugsnag bugsnag, ClientProperties clientProperties) {
    this.bugsnag = bugsnag;
    defaultsentLogLines = clientProperties.getBugsnagConfig().getLogLinesSent();
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        silentlyReport(e, t.getName());
      }
    });
  }

  /**
   * Opens Dialog for support
   */
  public void supportRequest() {
    report(null);
  }

  /**
   * Starts the reporting process and opens dialog
   *
   * @param exception can also be null, in this case it is a support request
   */
  public void report(@Nullable Throwable exception) {
    if (reportDialogListener != null) {
      reportDialogListener.display(defaultsentLogLines, exception);
    } else {
      log.error("Report can not be filled out as listener was not yet set");
    }
  }

  private void silentlyReport(Throwable e, String threadName) {
    silentlyReport(Severity.ERROR, e, defaultsentLogLines, false, null, null, threadName);
  }

  public void silentlyReport(Exception exception) {
    silentlyReport(Severity.ERROR, exception, defaultsentLogLines, true);
  }

  public void silentlyReport(Throwable exception, boolean handled) {
    silentlyReport(Severity.ERROR, exception, defaultsentLogLines, handled);
  }

  public void silentlyReport(Severity severity, Throwable exception, int sentLogLines, boolean handled) {
    silentlyReport(severity, exception, sentLogLines, handled, null, null, null);
  }

  private void silentlyReport(@NotNull Severity severity, @Nullable Throwable throwable, int sentLogLines, boolean handled, @Nullable String userFeedback, @Nullable Boolean youCanEmailMe, @Nullable String threadName) {
    if (throwable == null) {
      UUID uuid = UUID.randomUUID();
      String uuidString = uuid.toString();
      throwable = new SupportRequest(MessageFormat.format("Support request with UID: {0}", uuid));
      StackTraceElement[] trace = {new StackTraceElement("Reporting Service Support Request", uuidString, uuidString, 1)};
      throwable.setStackTrace(trace);
      log.debug("Sending support request", throwable);
    }

    try {
      bugsnag.notify(throwable, severity, report -> {
        if (!Strings.isNullOrEmpty(userFeedback)) {
          report.addToTab("User Given Information", "User feedback", userFeedback);
        }
        if (threadName != null) {
          report.addToTab("Thread", "Thread name", threadName);
        }
        if (youCanEmailMe != null) {
          report.addToTab("User Given Information", "User can be emailed", youCanEmailMe);
        }
        report.setContext(handled ? "handled" : "unhandled");
        if (sentLogLines > 0) {
          report.addToTab("Log", "log", readLogLines(sentLogLines));
        }
      });
    } catch (Exception e) {
      log.error("Failed to notify Bugsnag of error", throwable);
    }
  }

  /**
   * reads last count log lines
   *
   * @param count the number of lines to be read
   * @return the log as String
   */
  private String readLogLines(int count) {
    List<String> lines = new ArrayList<>();
    try {
      File logFile = Paths.get(System.getProperty("logging.file")).toFile();
      if (!logFile.exists()) {
        return "";
      }
      ReversedLinesFileReader reversedLinesFileReader = new ReversedLinesFileReader(logFile, Charset.defaultCharset());
      for (int i = 0; i < count; i++) {
        String line = reversedLinesFileReader.readLine();
        if (line == null) {
          break;
        }
        lines.add(line);
      }
    } catch (Exception e) {
      log.warn("Error while reading log", e);
    }
    Collections.reverse(lines);
    return lines.stream().collect(Collectors.joining("\n"));
  }

  public void userFilledOutReportForm(int logLines, boolean youCanEmailMe, String feedback, Severity severity, Throwable exception) {
    silentlyReport(severity, exception, logLines, true, feedback, youCanEmailMe, null);
  }

  public void setReportDialogListener(ReportingDialogListener reportDialogListener) {
    this.reportDialogListener = reportDialogListener;
  }

  public interface ReportingDialogListener {
    void display(int defaultLogLines, Throwable exception);
  }

  private class SupportRequest extends Exception {
    private SupportRequest(String message) {
      super(message);
    }
  }
}
