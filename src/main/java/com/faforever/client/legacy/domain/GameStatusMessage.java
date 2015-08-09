package com.faforever.client.legacy.domain;

public class GameStatusMessage extends ClientMessage {

  public enum Status {
    ON("on"),
    OFF("off");
    private final String string;

    Status(String string) {
      this.string = string;
    }
  }

  public final String state;

  public GameStatusMessage(Status status) {
    command = "fa_state";
    state = status.string;
  }
}
