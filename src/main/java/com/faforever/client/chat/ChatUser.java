package com.faforever.client.chat;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.pircbotx.User;

public class ChatUser {

  public static ChatUser fromIrcUser(User user) {
    String username = user.getRealName() != null ? user.getRealName() : user.getLogin();
    return new ChatUser(username, user.isIrcop());
  }

  private StringProperty username;
  private BooleanProperty isModerator;

  public ChatUser(String username) {
    this(username, false);
  }

  public ChatUser(String username, boolean isModerator) {
    this.username = new SimpleStringProperty(username);
    this.isModerator = new SimpleBooleanProperty(isModerator);
  }

  public boolean isModerator() {
    return isModerator.get();
  }

  public String getUsername() {
    return username.get();
  }

  @Override
  public int hashCode() {
    return username.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ChatUser
        && username.equals(obj);
  }
}
