package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.ReplayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class WatchGameMenuItem extends AbstractMenuItem<PlayerBean> {

  private final I18n i18n;
  private final ReplayService replayService;
  private final NotificationService notificationService;

  @Override
  protected void onClicked(PlayerBean player) {
    Assert.notNull(player, "No player has been set");
    try {
      replayService.runLiveReplay(player.getGame().getId());
    } catch (Exception e) {
      log.error("Cannot display live replay", e);
      notificationService.addImmediateErrorNotification(e, "replays.live.loadFailure.message");
    }
  }

  @Override
  protected boolean isItemVisible(PlayerBean player) {
    return player != null && player.getStatus() == PlayerStatus.PLAYING;
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.viewLiveReplay");
  }
}
