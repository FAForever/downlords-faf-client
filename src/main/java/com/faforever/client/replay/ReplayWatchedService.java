package com.faforever.client.replay;

import com.faforever.client.preferences.Preferences;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class ReplayWatchedService {

  private final Preferences preferences;

  boolean wasReplayWatched(Integer replayId) {
    return preferences.getReplayHistory().getWatchedReplayMap().containsKey(replayId);
  }

  OffsetDateTime getReplayWatchedDateTime(Integer replayId) {
    if (wasReplayWatched(replayId)) {
      return preferences.getReplayHistory().getWatchedReplayMap().get(replayId);
    } else {
      return null;
    }
  }

  void updateReplayWatchHistory(Integer replayId) {
    preferences.getReplayHistory().getWatchedReplayMap().put(replayId, OffsetDateTime.now());
  }


}
