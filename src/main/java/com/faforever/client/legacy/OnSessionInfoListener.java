package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.SessionInfo;

public interface OnSessionInfoListener {

  void onSessionInitiated(SessionInfo message);
}
