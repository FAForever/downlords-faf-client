package com.faforever.client.legacy;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class Peer {

  public int uid;
  public boolean connected;
  public InetSocketAddress inetSocketAddress;
  public DatagramSocket localSocket;
  public byte[] connectionTag;
  public byte[] ourConnectionTag;
  public boolean ourConnectionTagAcknowledged;
  public int numberOfReconnectionAttempts;
  public boolean currentlyReconnecting;
  public boolean ourConnectionTagDeclined;
  public long tagOfferTimestamp;
  public int numberOfTagOffers;

  @Override
  public String toString() {
    return inetSocketAddress.toString();
  }
}
