package com.faforever.client.replay;

import com.faforever.client.preferences.Preferences;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class ReplayWatchedService {

  private final Preferences preferences;

  private final IntegerProperty replayIdJustWatched = new SimpleIntegerProperty();

  boolean wasReplayWatched(Integer replayId) {
    return preferences.getReplayHistory().getWatchedReplayMap().contains(replayId);
  }

  void updateReplayWatchHistory(Integer replayId) {
    if (!wasReplayWatched(replayId)) {
      preferences.getReplayHistory().getWatchedReplayMap().add(replayId);
      replayIdJustWatched.set(replayId);
    }
  }

  public IntegerProperty replayIdJustWatchedProperty() {
    return replayIdJustWatched;
  }

}
