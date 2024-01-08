package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.Emoticon;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ChatMessage {

  @EqualsAndHashCode.Include
  @Getter
  private final String id;
  @Getter
  private final Instant time;
  @Getter
  private final ChatChannelUser sender;
  @Getter
  private final String content;
  @Getter
  private final Type type;
  @Getter
  private final ChatMessage targetMessage;

  private final ObservableMap<Emoticon, ObservableSet<String>> reactions = FXCollections.synchronizedObservableMap(
      FXCollections.observableHashMap());
  private final ObservableMap<Emoticon, ObservableSet<String>> unmodifiableReactions = FXCollections.unmodifiableObservableMap(
      reactions);

  public ObservableMap<Emoticon, ObservableSet<String>> getReactions() {
    return unmodifiableReactions;
  }

  public void addReaction(Emoticon reaction, ChatChannelUser reactor) {
    reactions.computeIfAbsent(reaction,
                              ignored -> FXCollections.synchronizedObservableSet(FXCollections.observableSet()))
             .add(reactor.getUsername());
  }

  public enum Type {
    MESSAGE, ACTION, PENDING
  }
}
