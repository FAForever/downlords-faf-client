package com.faforever.client.legacy.proxy;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class Peer {

  private int uid;
  private boolean connected;
  private InetSocketAddress inetSocketAddress;
  private DatagramSocket localSocket;
  private byte[] connectionTag;
  private byte[] ourConnectionTag;
  private boolean ourConnectionTagAcknowledged;
  private int numberOfReconnectionAttempts;
  private boolean currentlyReconnecting;
  private boolean ourConnectionTagDeclined;
  private long tagOfferTimestamp;
  private int numberOfTagOffers;

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

  public byte[] getConnectionTag() {
    return connectionTag;
  }

  public void setConnectionTag(byte[] connectionTag) {
    this.connectionTag = connectionTag;
  }

  public byte[] getOurConnectionTag() {
    return ourConnectionTag;
  }

  public void setOurConnectionTag(byte[] ourConnectionTag) {
    this.ourConnectionTag = ourConnectionTag;
  }

  public boolean isOurConnectionTagAcknowledged() {
    return ourConnectionTagAcknowledged;
  }

  public void setOurConnectionTagAcknowledged(boolean ourConnectionTagAcknowledged) {
    this.ourConnectionTagAcknowledged = ourConnectionTagAcknowledged;
  }

  public int getNumberOfReconnectionAttempts() {
    return numberOfReconnectionAttempts;
  }

  public void setNumberOfReconnectionAttempts(int numberOfReconnectionAttempts) {
    this.numberOfReconnectionAttempts = numberOfReconnectionAttempts;
  }

  public boolean isCurrentlyReconnecting() {
    return currentlyReconnecting;
  }

  public void setCurrentlyReconnecting(boolean currentlyReconnecting) {
    this.currentlyReconnecting = currentlyReconnecting;
  }

  public boolean isOurConnectionTagDeclined() {
    return ourConnectionTagDeclined;
  }

  public void setOurConnectionTagDeclined(boolean ourConnectionTagDeclined) {
    this.ourConnectionTagDeclined = ourConnectionTagDeclined;
  }

  public long getTagOfferTimestamp() {
    return tagOfferTimestamp;
  }

  public void setTagOfferTimestamp(long tagOfferTimestamp) {
    this.tagOfferTimestamp = tagOfferTimestamp;
  }

  public int getNumberOfTagOffers() {
    return numberOfTagOffers;
  }

  public void setNumberOfTagOffers(int numberOfTagOffers) {
    this.numberOfTagOffers = numberOfTagOffers;
  }
}
