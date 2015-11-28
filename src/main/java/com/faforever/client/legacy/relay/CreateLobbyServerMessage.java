package com.faforever.client.legacy.relay;

import com.faforever.client.util.Validator;

public class CreateLobbyServerMessage extends GpgServerMessage {

  private static final int LOBBY_MODE_INDEX = 0;
  private static final int PORT_INDEX = 1;
  private static final int USERNAME_INDEX = 2;
  private static final int UID_INDEX = 3;
  private static final int UNKNOWN_FLAG_INDEX = 4;

  public CreateLobbyServerMessage(LobbyMode lobbyMode, int port, String username, int uid, int unknownFlag) {
    super(GpgServerMessageType.CREATE_LOBBY, 5);
    Validator.notNull(lobbyMode, "lobbyMode must not be null");
    Validator.notNull(username, "username must not be null");

    setLobbyMode(lobbyMode);
    setPort(port);
    setUid(uid);
    setUsername(username);
    setUnknownFlag(unknownFlag);
  }

  public void setLobbyMode(LobbyMode lobbyMode) {
    setValue(LOBBY_MODE_INDEX, lobbyMode.getMode());
  }

  public void setPort(int port) {
    setValue(PORT_INDEX, port);
  }

  public void setUsername(String username) {
    setValue(USERNAME_INDEX, username);
  }

  public void setUnknownFlag(int unknownFlag) {
    setValue(UNKNOWN_FLAG_INDEX, unknownFlag);
  }

  /**
   * Returns the UID of the hosting player.
   */
  public int getUid() {
    return getInt(UID_INDEX);
  }

  public void setUid(int uid) {
    setValue(UID_INDEX, uid);
  }
}
