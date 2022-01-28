package com.faforever.client.fx.contextmenu;

import com.faforever.client.domain.GameBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.replay.LiveReplayService;
import com.faforever.client.replay.LiveReplayService.LiveReplayAction;
import javafx.util.Pair;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.faforever.client.replay.LiveReplayService.LiveReplayAction.NOTIFY_ME;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class NotifyMeMenuItem extends AbstractMenuItem<GameBean> {

  private final I18n i18n;
  private final LiveReplayService liveReplayService;

  @Override
  protected void onClicked() {
    liveReplayService.performActionWhenAvailable(object, NOTIFY_ME);
  }

  @Override
  protected boolean isItemVisible() {
    boolean isValid = object != null && object.getStartTime() != null;
    if (!isValid) {
      return false;
    }

    Optional<Pair<Integer, LiveReplayAction>> trackingReplayOptional = liveReplayService.getTrackingReplay();
    return trackingReplayOptional.isEmpty() || trackingReplayOptional.stream()
        .anyMatch(trackingReplay -> !trackingReplay.getKey().equals(object.getId()) || trackingReplay.getValue() != NOTIFY_ME);
  }

  @Override
  protected String getItemText() {
    return i18n.get("vault.liveReplays.contextMenu.notifyMe");
  }
}
