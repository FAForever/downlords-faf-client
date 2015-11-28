package com.faforever.client.legacy.proxy;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class Peer {

  private int uid;
  private boolean connected;
  private InetSocketAddress inetSocketAddress;
  private DatagramSocket localSocket;

  @Override
  public String toString() {
    return getInetSocketAddress().toString();
  }

  public InetSocketAddress getInetSocketAddress() {
    return inetSocketAddress;
  }

  public void setInetSocketAddress(InetSocketAddress inetSocketAddress) {
    this.inetSocketAddress = inetSocketAddress;
  }

  public int getUid() {
    return uid;
  }

  public void setUid(int uid) {
    this.uid = uid;
  }

  public boolean isConnected() {
    return connected;
  }

  public void setConnected(boolean connected) {
    this.connected = connected;
  }

  public DatagramSocket getLocalSocket() {
    return localSocket;
  }

  public void setLocalSocket(DatagramSocket localSocket) {
    this.localSocket = localSocket;
  }
}
