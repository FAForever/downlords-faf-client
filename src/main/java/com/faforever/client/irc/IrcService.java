package com.faforever.client.irc;

public interface IrcService {

  void addOnMessageListener(OnMessageListener listener);

  void addOnConnectedListener(OnConnectedListener listener);

  void addOnServerResponseListener(OnServerResponseListener listener);

  void addOnDisconnectedListener(OnDisconnectedListener listener);

  void addOnPrivateMessageListener(OnPrivateMessageListener listener);

  void addOnChannelJoinedListener(OnChannelJoinedListener listener);

  void connect();

  void sendMessage(String target, String message);
}
