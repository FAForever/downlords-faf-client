package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.SessionMessageLobby;

public interface OnSessionInfoListener {

  void onSessionInitiated(SessionMessageLobby message);
}
