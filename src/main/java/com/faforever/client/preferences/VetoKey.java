package com.faforever.client.preferences;

public record VetoKey(int matchmakerQueueMapPoolId, int mapPoolMapVersionId) {

  @Override
  public String toString() {
    return matchmakerQueueMapPoolId + ":" + mapPoolMapVersionId;
  }

  public static VetoKey fromString(String key) {
    String[] parts = key.split(":");
    return new VetoKey(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
  }
}