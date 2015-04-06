package com.faforever.client.chat;

public interface ChatService {

  void addOnMessageListener(OnMessageListener listener);

  void addOnConnectedListener(OnConnectedListener listener);

  void addOnServerResponseListener(OnServerResponseListener listener);

  void addOnUserListListener(OnUserListListener listener);

  void addOnDisconnectedListener(OnDisconnectedListener listener);

  void addOnPrivateMessageListener(OnPrivateMessageListener listener);

  void addOnUserJoinedListener(OnUserJoinedListener listener);

  void addOnUserLeftListener(OnUserLeftListener listener);

  void connect();

  void sendMessage(String target, String message);
}
