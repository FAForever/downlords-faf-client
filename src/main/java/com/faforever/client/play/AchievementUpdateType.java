package com.faforever.client.play;

import com.google.api.client.util.Value;

public enum AchievementUpdateType {
  @Value REVEAL,
  @Value INCREMENT,
  @Value SET_STEPS_AT_LEAST,
  @Value UNLOCK
}
