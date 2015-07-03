package com.faforever.client.legacy.relay;

public class JoinGameMessage extends RelayServerMessage{

  private static final int PEER_ADDRESS_INDEX = 0;
  private static final int USERNAME_INDEX = 1;
  private static final int PEER_UID_INDEX = 2;

  public JoinGameMessage() {
    super(RelayServerCommand.JOIN_GAME);
  }

  public String getPeerAddress() {
    return (String) getArgs().get(PEER_ADDRESS_INDEX);
  }

  public int getPeerUid() {
    return asInt(getArgs().get(PEER_UID_INDEX));
  }

  public void setPeerAddress(String peerAddress) {
    getArgs().set(PEER_ADDRESS_INDEX, peerAddress);
  }

  public void setUsername(String username) {
    commands.set(USERNAME_INDEX, username);
  }

  public void setPeerUid(int peerUid) {
    commands.set(PEER_UID_INDEX, peerUid);
  }
}
