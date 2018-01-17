package com.faforever.client.reporting;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.update.Version;
import com.faforever.client.user.UserService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.text.MessageFormat;

@Slf4j
@Lazy
@Service
public class SupportServiceImpl implements SupportService {

  private static final int DEFAULT_READ_LOG_LINES = 100;
  private static final String MAIL_SUBJECT = "Support request from {0}";
  private static final String MAIL_TEMPLATE = "Dear Developer Team,\n" +
      "\n" +
      "I have encountered the following problem:\n" +
      "\n" +
      "**********************************************\n" +
      "**please tell us what problem you have**\n" +
      "**********************************************\n" +
      "\n" +
      "I would have expected:\n" +
      "\n" +
      "*************************************************************\n" +
      "**please tell what you would have expected instead**\n" +
      "*************************************************************\n" +
      "\n" +
      "Greetings {0}\n" +
      "\n" +
      "---------------------------------------------------------------\n" +
      "username: {0}\n" +
      "clientVersion: {1}\n" +
      "supportType: {2}\n" +
      "system: {3}\n" +
      "\n" +
      "error message:\n" +
      "{4}\n" +
      "\n" +
      "lastLogStatements:\n" +
      "{5}";
  private final PlatformService platformService;
  private final ClientProperties clientProperties;
  private final UserService userService;
  private final PreferencesService preferencesService;

  public SupportServiceImpl(PlatformService platformService, ClientProperties clientProperties, UserService userService, PreferencesService preferencesService) {
    this.platformService = platformService;
    this.clientProperties = clientProperties;
    this.userService = userService;
    this.preferencesService = preferencesService;
  }

  @Override
  public void reportError(Throwable e) {
    String url = generateMailToLink(userService.getUsername(), SupportType.REPORT_ERROR, e, DEFAULT_READ_LOG_LINES);
    platformService.showDocument(url);
    log.info("Reporting throwable with link: " + url, e);
  }

  @Override
  public void requestSupport() {
    String url = generateMailToLink(userService.getUsername(), SupportType.REQUEST_SUPPORT, null, DEFAULT_READ_LOG_LINES);
    callMailUrl(url);
    platformService.showDocument(url);
    log.debug("Support was requested with link: " + url);
  }

  private void callMailUrl(String url) {
    platformService.showDocument(url);
  }

  @SneakyThrows
  private String generateMailToLink(String username, SupportType supportType, Throwable throwable, int logLines) {
    String body = MessageFormat.format(MAIL_TEMPLATE, username, Version.VERSION, supportType.name(), System.getProperty("os.name"), throwable != null ? throwable : "none specified", readLogLines(logLines));
    String subject = MessageFormat.format(MAIL_SUBJECT, username);

    return String.format(clientProperties.getSupport().getMailToFormat(), URLEncoder.encode(subject, "UTF-8"), URLEncoder.encode(body, "UTF-8"));
  }

  private String readLogLines(int count) {
    StringBuilder logLines = new StringBuilder();
    try {
      BufferedReader bufferedReader = Files.newBufferedReader(preferencesService.getClientLogFilePath());
      for (int i = 0; bufferedReader.ready() && i < count; i++) {
        logLines.append(bufferedReader.readLine() + "\n");
      }
    } catch (Exception e) {
      log.warn("Error when reading log", e);
    }
    return logLines.toString();
  }

  private enum SupportType {
    REQUEST_SUPPORT,
    REPORT_ERROR
  }
}
