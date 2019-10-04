package com.faforever.client.main.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HostGameEvent extends OpenCustomGamesEvent {
  private String mapFolderName;

  public HostGameEvent(String mapFolderName) {
    this.mapFolderName = mapFolderName;
  }
}
