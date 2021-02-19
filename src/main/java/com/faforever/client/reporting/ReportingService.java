package com.faforever.client.reporting;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.util.ClipboardUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;


@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class ReportingService {
  private final ClientProperties clientProperties;
  private final PlatformService platformService;

  public void getHelp() {
    platformService.showDocument(clientProperties.getLinks().get("linksTecHelpForum"));
  }

  public void copyError(Throwable throwable) {
    StringWriter writer = new StringWriter();
    throwable.printStackTrace(new PrintWriter(writer));

    ClipboardUtil.copyToClipboard(writer.toString());
  }
}
