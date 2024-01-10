package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.Emoticon;
import com.faforever.client.chat.emoticons.Reaction;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
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

  private final ObservableMap<Emoticon, ObservableMap<String, String>> reactions = FXCollections.synchronizedObservableMap(
      FXCollections.observableHashMap());
  private final ObservableMap<Emoticon, ObservableMap<String, String>> unmodifiableReactions = FXCollections.unmodifiableObservableMap(
      reactions);

  public ObservableMap<Emoticon, ObservableMap<String, String>> getReactions() {
    return unmodifiableReactions;
  }

  public void addReaction(Reaction reaction) {
    reactions.computeIfAbsent(reaction.emoticon(),
                              ignored -> FXCollections.synchronizedObservableMap(FXCollections.observableHashMap()))
             .put(reaction.reactorName(), reaction.messageId());
  }

  public void removeReaction(Reaction reaction) {
    ObservableMap<String, String> reactors = reactions.getOrDefault(reaction.emoticon(),
                                                                    FXCollections.emptyObservableMap());
    reactors.remove(reaction.reactorName());
    if (reactors.isEmpty()) {
      reactions.remove(reaction.emoticon());
    }
  }

  public enum Type {
    MESSAGE, ACTION, PENDING
  }
}
