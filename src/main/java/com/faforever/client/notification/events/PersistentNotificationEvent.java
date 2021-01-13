package com.faforever.client.notification.events;

import com.faforever.client.notification.Action;
import com.faforever.client.notification.Severity;
import lombok.Data;

import java.util.List;

@Data
public class PersistentNotificationEvent {
  private final String text;
  private final Severity severity;
  private final List<Action> actions;
}
