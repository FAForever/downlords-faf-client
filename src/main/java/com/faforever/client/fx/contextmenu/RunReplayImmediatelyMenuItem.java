package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.replay.LiveReplayService;
import com.faforever.client.replay.TrackingLiveReplay;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.faforever.client.replay.TrackingLiveReplayAction.RUN_REPLAY;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class RunReplayImmediatelyMenuItem extends AbstractMenuItem<GameInfo> {

  private final I18n i18n;
  private final LiveReplayService liveReplayService;

  @Override
  protected void onClicked() {
    liveReplayService.performActionWhenAvailable(object, RUN_REPLAY);
  }

  @Override
  protected String getStyleIcon() {
    return "play-circle-outline-icon";
  }

  @Override
  protected boolean isDisplayed() {
    boolean isValid = object != null && object.getStartTime() != null;
    if (!isValid) {
      return false;
    }

    Optional<TrackingLiveReplay> trackingLiveReplayOptional = liveReplayService.getTrackingLiveReplay();
    return trackingLiveReplayOptional.isEmpty() || trackingLiveReplayOptional.stream()
        .anyMatch(trackingLiveReplay -> !trackingLiveReplay.gameId().equals(object.getId()) || trackingLiveReplay.action() != RUN_REPLAY);
  }

  @Override
  protected String getItemText() {
    return i18n.get("vault.liveReplays.contextMenu.runReplayImmediately");
  }
}
