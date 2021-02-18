package com.faforever.client.reporting;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;


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
}
