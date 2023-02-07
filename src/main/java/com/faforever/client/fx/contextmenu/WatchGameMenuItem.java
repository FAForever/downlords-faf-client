package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.LiveReplayService;
import com.faforever.client.util.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class WatchGameMenuItem extends AbstractMenuItem<PlayerBean> {

  private final I18n i18n;
  private final LiveReplayService liveReplayService;
  private final NotificationService notificationService;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "no player has been set");
    try {
      liveReplayService.runLiveReplay(object.getGame().getId());
    } catch (Exception e) {
      log.error("Cannot display live replay", e);
      notificationService.addImmediateErrorNotification(e, "replays.live.loadFailure.message");
    }
  }

  @Override
  protected String getStyleIcon() {
    return "play-circle-outline-icon";
  }

  @Override
  protected boolean isDisplayed() {
    return object != null && object.getStatus() == PlayerStatus.PLAYING;
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.viewLiveReplay");
  }
}
