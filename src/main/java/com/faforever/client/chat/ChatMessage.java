package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.Emoticon;
import com.faforever.client.chat.emoticons.Reaction;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Optional;

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
  private final ChatMessage targetMessage;

  private final BooleanProperty seen = new SimpleBooleanProperty();
  private final ObservableMap<Emoticon, ObservableMap<String, String>> reactions = FXCollections.synchronizedObservableMap(
      FXCollections.observableHashMap());
  private final ObservableMap<Emoticon, ObservableMap<String, String>> unmodifiableReactions = FXCollections.unmodifiableObservableMap(
      reactions);

  public Optional<ChatMessage> getTargetMessage() {
    return Optional.ofNullable(targetMessage);
  }

  public ObservableMap<Emoticon, ObservableMap<String, String>> getReactions() {
    return unmodifiableReactions;
  }

  public void addReaction(Reaction reaction) {
    reactions.computeIfAbsent(reaction.emoticon(),
                              _ -> FXCollections.synchronizedObservableMap(FXCollections.observableHashMap()))
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

  public boolean isSeen() {
    return seen.get();
  }

  public BooleanProperty seenProperty() {
    return seen;
  }

  public void setSeen(boolean seen) {
    this.seen.set(seen);
  }

  public enum Type {
    MESSAGE, ACTION, PENDING
  }
}
