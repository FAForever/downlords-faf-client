package com.faforever.client.main.event;

import lombok.Getter;
import lombok.Setter;

public class ShowMapPoolEvent extends OpenMapVaultEvent {
  @Getter
  @Setter
  private int queueId;

  public ShowMapPoolEvent(int id) {
    super();
    queueId = id;
  }
}