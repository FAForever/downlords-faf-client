package com.faforever.client.remote.domain;

import java.net.InetSocketAddress;

public class JoinGameMessage extends ClientMessage {

  private Integer uid;
  private String password;
  private Integer gameport;
  private InetSocketAddress relayAddress;

  public JoinGameMessage(int uid, int port, String password, InetSocketAddress relayAddress) {
    super(ClientMessageType.JOIN_GAME);
    this.setRelayAddress(relayAddress);
    this.setUid(uid);
    this.setPassword(password);
    this.setGameport(port);
  }

  public Integer getUid() {
    return uid;
  }

  public void setUid(Integer uid) {
    this.uid = uid;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Integer getGameport() {
    return gameport;
  }

  public void setGameport(Integer gameport) {
    this.gameport = gameport;
  }

  public InetSocketAddress getRelayAddress() {
    return relayAddress;
  }

  public void setRelayAddress(InetSocketAddress relayAddress) {
    this.relayAddress = relayAddress;
  }
}
