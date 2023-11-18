package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.ui.preferences.GameDirectoryRequiredHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MissingGamePathNotifier {

  private final I18n i18n;
  private final NotificationService notificationService;
  private final GameDirectoryRequiredHandler gameDirectoryRequiredHandler;

  public void onMissingGamePathEvent(boolean immediateUserActionRequired) {
    List<Action> actions = Collections.singletonList(
        new Action(i18n.get("missingGamePath.locate"),
                   chooseEvent -> gameDirectoryRequiredHandler.onChooseGameDirectory(null))
    );
    String notificationText = i18n.get("missingGamePath.notification");

    if (immediateUserActionRequired) {
      notificationService.addNotification(new ImmediateNotification(notificationText, notificationText, Severity.WARN, actions));
    } else {
      notificationService.addNotification(new PersistentNotification(notificationText, Severity.WARN, actions));
    }
  }
}
