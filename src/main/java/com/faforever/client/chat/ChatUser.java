package com.faforever.client.chat;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.pircbotx.User;

public class ChatUser {

  private StringProperty username;

  public ChatUser(String username) {
    this.username = new SimpleStringProperty(username);
  }

  public String getUsername() {
    return username.get();
  }

  public StringProperty usernameProperty() {
    return username;
  }

  public void setUsername(String username) {
    this.username.set(username);
  }
}
