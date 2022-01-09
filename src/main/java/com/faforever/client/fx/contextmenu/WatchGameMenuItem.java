package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.ReplayService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WatchGameMenuItem extends AbstractMenuItem<PlayerBean> {

  @Override
  protected void onClicked(PlayerBean player) {
    try {
      getBean(ReplayService.class).runLiveReplay(player.getGame().getId());
    } catch (Exception e) {
      log.error("Cannot display live replay", e);
      getBean(NotificationService.class).addImmediateErrorNotification(e, "replays.live.loadFailure.message");
    }
  }

  @Override
  protected boolean isItemVisible() {
    return getObject().getStatus() == PlayerStatus.PLAYING;
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.viewLiveReplay");
  }
}
