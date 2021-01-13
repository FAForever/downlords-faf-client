package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.Severity;
import com.faforever.client.notification.events.ImmediateNotificationEvent;
import com.faforever.client.notification.events.PersistentNotificationEvent;
import com.faforever.client.preferences.event.MissingGamePathEvent;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MissingGamePathNotifier implements InitializingBean {

  private final EventBus eventBus;
  private final I18n i18n;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  @Subscribe
  public void onMissingGamePathEvent(MissingGamePathEvent event) {
    List<Action> actions = Collections.singletonList(
        new Action(i18n.get("missingGamePath.locate"), chooseEvent -> eventBus.post(new GameDirectoryChooseEvent()))
    );
    String notificationText = i18n.get("missingGamePath.notification");

    if (event.isImmediateUserActionRequired()) {
      eventBus.post(new ImmediateNotificationEvent(notificationText, notificationText, Severity.WARN, actions));
    } else {
      eventBus.post(new PersistentNotificationEvent(notificationText, Severity.WARN, actions));
    }
  }
}
