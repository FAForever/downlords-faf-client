package com.faforever.client.replay;

import com.faforever.client.replay.Replay.ChatMessage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ReplayChatMessageListBuilder {
  private final List<ChatMessage> chatMessages = new ArrayList<>();

  public static ReplayChatMessageListBuilder create() {
    return new ReplayChatMessageListBuilder();
  }

  public ReplayChatMessageListBuilder defaultValues() {
    append(new ChatMessage(Duration.ZERO, "junit1", "hf gl"));
    append(new ChatMessage(Duration.ofSeconds(10), "junit2", "u2"));
    return this;
  }

  public ReplayChatMessageListBuilder append(ChatMessage chatMessage) {
    chatMessages.add(chatMessage);
    return this;
  }

  public ReplayChatMessageListBuilder replace(List<ChatMessage> chatMessages) {
    this.chatMessages.clear();
    this.chatMessages.addAll(chatMessages);
    return this;
  }

  public List<ChatMessage> get() {
    return chatMessages;
  }
}
