package com.faforever.client.notification;

import com.faforever.client.i18n.I18n;
import com.faforever.client.replay.LiveReplayService;

public class RunReplayAction extends Action {

  public RunReplayAction(I18n i18n, ActionCallback callback) {
    super(i18n.get("game.watch"), callback);
  }
}
