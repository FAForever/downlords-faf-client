package com.faforever.client.chat;

import com.google.common.collect.ImmutableSortedSet;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.scene.paint.Color;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.UserLevel;

import java.util.HashSet;
import java.util.Set;

public class ChatUser {

  private StringProperty username;
  private SetProperty<String> moderatorInChannels;

  private Color color;

  public ChatUser(String username) {
    this(username, new HashSet<>());
    this.color = ColorGeneratorUtil.generateRandomHexColor();
  }

  public ChatUser(String username, Set<String> moderatorInChannels) {
    this.username = new SimpleStringProperty(username);
    this.moderatorInChannels = new SimpleSetProperty<>(FXCollections.observableSet(moderatorInChannels));
    this.color = ColorGeneratorUtil.generateRandomHexColor();

  }

  public ObservableSet<String> getModeratorInChannels() {
    return moderatorInChannels.get();
  }

  public SetProperty<String> moderatorInChannelsProperty() {
    return moderatorInChannels;
  }

  public String getUsername() {
    return username.get();
  }

  public StringProperty usernameProperty() {
    return username;
  }

  public Color getColor() {
    return color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  @Override
  public int hashCode() {
    return username.get().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ChatUser
        && username.get().equals(((ChatUser) obj).username.get());
  }

  public static ChatUser fromIrcUser(User user) {
    String username = user.getNick() != null ? user.getNick() : user.getLogin();

    Set<String> moderatorInChannels = new HashSet<>();
    for (Channel channel : user.getChannels()) {
      ImmutableSortedSet<UserLevel> userLevels = user.getUserLevels(channel);
      for (UserLevel userLevel : userLevels) {
        switch (userLevel) {
          case OP:
          case HALFOP:
          case SUPEROP:
          case OWNER:
            moderatorInChannels.add(channel.getName());
            break;

          default:
            // Nothing special
        }
      }
    }
    return new ChatUser(username, moderatorInChannels);
  }
}
