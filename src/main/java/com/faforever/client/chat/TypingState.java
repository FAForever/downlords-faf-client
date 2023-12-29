package com.faforever.client.chat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TypingState {
  ACTIVE("active"), PAUSED("paused"), DONE("done");

  private final String value;
}
