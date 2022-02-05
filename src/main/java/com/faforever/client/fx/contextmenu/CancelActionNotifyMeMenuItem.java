package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.GameBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.replay.LiveReplayService;
import com.faforever.client.replay.TrackingLiveReplay;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.faforever.client.replay.TrackingLiveReplayAction.NOTIFY_ME;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class CancelActionNotifyMeMenuItem extends AbstractMenuItem<GameBean> {

  private final I18n i18n;
  private final LiveReplayService liveReplayService;

  @Override
  protected void onClicked() {
    liveReplayService.stopTrackingLiveReplay();
  }

  @Override
  protected boolean isItemVisible() {
    boolean isValid = object != null && object.getStartTime() != null;
    if (!isValid) {
      return false;
    }

    Optional<TrackingLiveReplay> trackingLiveReplayOptional = liveReplayService.getTrackingLiveReplay();
    return trackingLiveReplayOptional.stream().anyMatch(trackingLiveReplay -> trackingLiveReplay.getGameId().equals(object.getId())
        && trackingLiveReplay.getAction() == NOTIFY_ME);
  }

  @Override
  protected String getItemText() {
    return i18n.get("vault.liveReplays.contextMenu.notifyMe.cancel");
  }
}
