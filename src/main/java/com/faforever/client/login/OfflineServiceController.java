package com.faforever.client.login;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.util.TimeService;
import javafx.scene.control.Label;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class OfflineServiceController implements Controller<Label> {

  public Label offlineServiceRoot;

  private final I18n i18n;
  private final TimeService timeService;

  @Override
  public Label getRoot() {
    return offlineServiceRoot;
  }

  public void setInfo(String serviceName, String reason, OffsetDateTime lastSeen) {
    Duration offlineSince = Duration.between(lastSeen, OffsetDateTime.now());
    offlineServiceRoot.setText(i18n.get("login.offlineService.text", serviceName, timeService.shortDuration(offlineSince), reason));
  }
}
