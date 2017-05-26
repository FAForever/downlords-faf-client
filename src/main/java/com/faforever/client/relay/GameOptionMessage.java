package com.faforever.client.relay;

import com.faforever.client.fa.relay.GpgServerMessage;
import com.faforever.client.fa.relay.GpgServerMessageType;

public class GameOptionMessage extends GpgServerMessage {

  private static final int NAME_INDEX = 0;
  private static final int VALUE_INDEX = 1;

  public GameOptionMessage(String name, Object value) {
    super(GpgServerMessageType.GAME_OPTION, 2);
    setName(name);
    setValue(value);
  }

  public GameOptionMessage() {
    super(GpgServerMessageType.GAME_OPTION, 2);
  }

  public void setName(String name) {
    setValue(NAME_INDEX, name);
  }

  public void setValue(Object value) {
    setValue(VALUE_INDEX, value);
  }
}
