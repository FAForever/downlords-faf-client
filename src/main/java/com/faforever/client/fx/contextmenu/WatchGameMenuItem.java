package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.ReplayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

@Slf4j
public class WatchGameMenuItem extends AbstractMenuItem<PlayerBean> {

  @Override
  protected void onClicked(PlayerBean player) {
    Assert.notNull(player, "No player has been set");
    try {
      getBean(ReplayService.class).runLiveReplay(player.getGame().getId());
    } catch (Exception e) {
      log.error("Cannot display live replay", e);
      getBean(NotificationService.class).addImmediateErrorNotification(e, "replays.live.loadFailure.message");
    }
  }

  @Override
  protected boolean isItemVisible(PlayerBean player) {
    return player != null && player.getStatus() == PlayerStatus.PLAYING;
  }

  @Override
  protected String getItemText(I18n i18n) {
    return i18n.get("chat.userContext.viewLiveReplay");
  }
}
