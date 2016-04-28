package com.faforever.client.chat;

import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Channel {

  private final ObservableMap<String, ChatUser> users;
  private String name;

  public Channel(String name) {
    this.name = name;
    users = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());
  }

  public void removeUser(String username) {
    users.remove(username);
  }

  public void addUsers(List<ChatUser> users) {
    users.forEach(user -> this.users.put(user.getUsername(), user));
  }

  public void addUser(ChatUser chatUser) {
    users.put(chatUser.getUsername(), chatUser);
  }

  public void clearUsers() {
    users.clear();
  }

  public void addUsersListeners(MapChangeListener<String, ChatUser> listener) {
    users.addListener(listener);
  }

  public void removeUserListener(MapChangeListener<String, ChatUser> listener) {
    users.removeListener(listener);
  }

  public void setModerator(String username) {
    ChatUser chatUser = users.get(username);
    if (chatUser != null) {
      chatUser.getModeratorInChannels().add(name);
    }
  }

  /**
   * Returns an unmodifiable copy of the current users.
   */
  public List<ChatUser> getUsers() {
    return Collections.unmodifiableList(new ArrayList<>(users.values()));
  }

  public ChatUser getUser(String username) {
    return users.get(username);
  }

  public String getName() {
    return name;
  }
}
