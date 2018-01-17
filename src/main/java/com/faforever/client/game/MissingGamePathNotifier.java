package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.notification.notificationEvents.ShowImmediateNotificationEvent;
import com.faforever.client.notification.notificationEvents.ShowPersistentNotificationEvent;
import com.faforever.client.preferences.event.MissingGamePathEvent;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@Component
public class MissingGamePathNotifier {

  private final EventBus eventBus;
  private final I18n i18n;
  private final ApplicationEventPublisher applicationEventPublisher;

  @Inject
  public MissingGamePathNotifier(EventBus eventBus, I18n i18n, ApplicationEventPublisher applicationEventPublisher) {
    this.eventBus = eventBus;
    this.i18n = i18n;
    this.applicationEventPublisher = applicationEventPublisher;
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
    String notificationText = i18n.get("missingGamePath.notification");

    if (event.isImmediateUserActionRequired()) {
      applicationEventPublisher.publishEvent(new ShowImmediateNotificationEvent(new ImmediateNotification(notificationText, notificationText, Severity.WARN, actions)));
    } else {
      applicationEventPublisher.publishEvent(new ShowPersistentNotificationEvent(new PersistentNotification(notificationText, Severity.WARN, actions)));
    }
  }
}
