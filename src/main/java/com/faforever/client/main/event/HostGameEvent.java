package com.faforever.client.main.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HostGameEvent extends OpenCustomGamesEvent {
  private String mapFolderName;
}
