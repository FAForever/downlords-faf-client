package com.faforever.client.legacy.relay;

public class CreateLobbyMessage extends RelayServerMessage {

  public static final int LOBBY_MODE_INDEX = 0;
  public static final int PORT_INDEX = 1;
  public static final int USERNAME_INDEX = 2;
  public static final int UID_INDEX = 3;

  public CreateLobbyMessage(LobbyMode lobbyMode, int port, String username, int uid) {
    setLobbyMode(lobbyMode);
    setPort(port);
    setUid(uid);
    setUsername(username);
  }

  /**
   * Returns the UID of the hosting player.
   */
  public int getUid() {
    return asInt(getArgs().get(UID_INDEX));
  }

  public void setPort(int port) {
    getArgs().set(PORT_INDEX, port);
  }

  public void setLobbyMode(LobbyMode lobbyMode) {
    getArgs().set(LOBBY_MODE_INDEX, lobbyMode.getMode());
  }

  public void setUsername(String username) {
    getArgs().set(USERNAME_INDEX, username);
  }

  public void setUid(int uid) {
    getArgs().set(UID_INDEX, uid);
  }

}
