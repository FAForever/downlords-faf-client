package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.SessionMessage;

public interface OnSessionInfoListener {

  void onSessionInitiated(SessionMessage message);
}
