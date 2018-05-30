package com.faforever.client.chat;

import com.faforever.client.player.Player;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;
import org.pircbotx.User;
import org.pircbotx.UserLevel;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Represents a chat user within a channel. If a user is in multiple channels, one instance per channel needs to be
 * created since e.g. the {@code isModerator} flag is specific to the channel.
 */
public class ChatChannelUser {

  protected static final List<UserLevel> MODERATOR_USER_LEVELS = Arrays.asList(UserLevel.OP, UserLevel.HALFOP, UserLevel.SUPEROP, UserLevel.OWNER);
  private final StringProperty username;
  private final BooleanProperty moderator;
  private final ObjectProperty<Color> color;
  private final ObjectProperty<Player> player;

  public ChatChannelUser(String username, Color color) {
    this(username, false, color);
  }

  ChatChannelUser(String username, boolean moderator, Color color) {
    this.username = new SimpleStringProperty(username);
    this.moderator = new SimpleBooleanProperty(moderator);
    this.color = new SimpleObjectProperty<>(color);
    this.player = new SimpleObjectProperty<>();
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

  public static ChatChannelUser fromIrcUser(User user, Color color, String targetChannel) {
    String username = user.getNick() != null ? user.getNick() : user.getLogin();

    boolean isModerator = user.getChannels().stream()
        .filter(channel -> channel.getName().equals(targetChannel))
        .flatMap(channel -> user.getUserLevels(channel).stream())
        .anyMatch(MODERATOR_USER_LEVELS::contains);

    return new ChatChannelUser(username, isModerator, color);
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

  @Override
  public boolean equals(Object obj) {
    return obj != null
        && obj.getClass() == this.getClass()
        && username.get().equalsIgnoreCase(((ChatChannelUser) obj).username.get());
  }
}
