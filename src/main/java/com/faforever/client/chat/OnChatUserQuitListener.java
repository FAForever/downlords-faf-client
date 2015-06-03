package com.faforever.client.chat;

public interface OnChatUserQuitListener {

  /**
   * Called when a user (possibly us) leaves the server.
   */
  void onChatUserQuit(String login);
}
