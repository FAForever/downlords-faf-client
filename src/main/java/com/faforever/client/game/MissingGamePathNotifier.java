package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.event.MissingGamePathEvent;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@Component
public class MissingGamePathNotifier {

  private final EventBus eventBus;
  private final I18n i18n;
  private final NotificationService notificationService;

  @Inject
  public MissingGamePathNotifier(EventBus eventBus, I18n i18n, NotificationService notificationService) {
    this.eventBus = eventBus;
    this.i18n = i18n;
    this.notificationService = notificationService;
  }

  @PostConstruct
  public void postConstruct() {
    eventBus.register(this);
  }

  @Subscribe
  public void onMissingGamePathEvent(MissingGamePathEvent event) {
    List<Action> actions = Collections.singletonList(
        new Action(i18n.get("missingGamePath.locate"), chooseEvent -> eventBus.post(new GameDirectoryChooseEvent()))
    );
    String message = i18n.get("missingGamePath.notification");
    notificationService.addNotification(new ImmediateNotification(message, message, Severity.WARN, actions));
  }
}
