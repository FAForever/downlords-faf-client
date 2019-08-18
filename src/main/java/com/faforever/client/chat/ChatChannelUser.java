package com.faforever.client.chat;

import com.faforever.client.player.Player;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;
import lombok.ToString;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a chat user within a channel. If a user is in multiple channels, one instance per channel needs to be
 * created since e.g. the {@code isModerator} flag is specific to the channel.
 */
@ToString
public class ChatChannelUser {

  private final StringProperty username;
  private final BooleanProperty moderator;
  private final ObjectProperty<Color> color;
  private final ObjectProperty<Player> player;
  private final ObjectProperty<Instant> lastActive;

  public ChatChannelUser(String username, Color color, boolean moderator) {
    this(username, color, moderator, null);
  }

  ChatChannelUser(String username, Color color, boolean moderator, Player player) {
    this.username = new SimpleStringProperty(username);
    this.moderator = new SimpleBooleanProperty(moderator);
    this.color = new SimpleObjectProperty<>(color);
    this.player = new SimpleObjectProperty<>(player);
    this.lastActive = new SimpleObjectProperty<>();
  }

  public Optional<Player> getPlayer() {
    return Optional.ofNullable(player.get());
  }

  public void setPlayer(Player player) {
    this.player.set(player);
  }

  public ObjectProperty<Player> playerProperty() {
    return player;
  }

  public Color getColor() {
    return color.get();
  }

  public void setColor(Color color) {
    this.color.set(color);
  }

  public ObjectProperty<Color> colorProperty() {
    return color;
  }

  public boolean isModerator() {
    return moderator.get();
  }

  public void setModerator(boolean moderator) {
    this.moderator.set(moderator);
  }

  public String getUsername() {
    return username.get();
  }

  public StringProperty usernameProperty() {
    return username;
  }

  @Override
  public int hashCode() {
    return username.get().hashCode();
  }

  public BooleanProperty moderatorProperty() {
    return moderator;
  }

  public Instant getLastActive() {
    return lastActive.get();
  }

  public void setLastActive(Instant lastActive) {
    this.lastActive.set(lastActive);
  }

  public ObjectProperty<Instant> lastActiveProperty() {
    return lastActive;
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null
        && obj.getClass() == this.getClass()
        && username.get().equalsIgnoreCase(((ChatChannelUser) obj).username.get());
  }

  Set<ChatUserCategory> getChatUserCategories() {
    Set<ChatUserCategory> userCategories = new HashSet<>();

    Optional<Player> playerOptional = Optional.ofNullable(player.get());
    if (!playerOptional.isPresent()) {
      userCategories.add(ChatUserCategory.CHAT_ONLY);
    } else {
      switch (playerOptional.get().getSocialStatus()) {
        case FRIEND:
          userCategories.add(ChatUserCategory.FRIEND);
          break;
        case FOE:
          userCategories.add(ChatUserCategory.FOE);
          break;
        case OTHER:
        case SELF:
          userCategories.add(ChatUserCategory.OTHER);
          break;
        default:
          // Nothing to do
      }
    }

    if (moderator.get()) {
      userCategories.add(ChatUserCategory.MODERATOR);
    }

    return userCategories;
  }
}
