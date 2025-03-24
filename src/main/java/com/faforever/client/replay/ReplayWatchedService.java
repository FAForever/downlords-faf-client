package com.faforever.client.replay;

import com.faforever.client.preferences.Preferences;
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

  boolean wasReplayWatched(Integer replayId) {
    return preferences.getReplayHistory().getWatchedReplayMap().contains(replayId);
  }

  void updateReplayWatchHistory(Integer replayId) {
    preferences.getReplayHistory().getWatchedReplayMap().add(replayId);
  }


}
