package com.faforever.client.chat;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class ChatMessage {

  @EqualsAndHashCode.Include
  private final String id;
  private final Instant time;
  private final ChatChannelUser sender;
  private final String content;
  private final Type type;

  public enum Type {
    MESSAGE, ACTION, PENDING
  }
}
