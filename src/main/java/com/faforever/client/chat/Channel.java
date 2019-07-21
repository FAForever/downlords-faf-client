package com.faforever.client.chat;

import com.faforever.client.fx.JavaFxUtil;
import javafx.beans.property.ReadOnlySetWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class Channel {

  // TODO make sure any code that touches the keys knows that they are lowercase
  private final ObservableMap<String, ChatChannelUser> usersByLowerName;
  private final StringProperty topic;
  private String name;
  private ObservableSet<String> moderators;

  public Channel(String name) {
    this.name = name;
    usersByLowerName = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());
    moderators = FXCollections.observableSet();
    topic = new SimpleStringProperty();
  }

  public String getTopic() {
    return topic.get();
  }

  public void setTopic(String topic) {
    this.topic.set(topic);
  }

  public StringProperty topicProperty() {
    return topic;
  }

  public ChatChannelUser removeUser(String username) {
    return usersByLowerName.remove(username.toLowerCase());
  }

  public void addUsers(List<ChatChannelUser> users) {
    users.forEach(this::addUser);
  }

  public void addUser(ChatChannelUser user) {
    if (moderators.contains(user.getUsername())) {
      user.setModerator(true);
    }
    usersByLowerName.put(user.getUsername().toLowerCase(), user);
  }

  public void clearUsers() {
    usersByLowerName.clear();
  }

  /**
   * Listen for lower case username to {@link ChatChannelUser} map
   */
  public void addUsersListeners(MapChangeListener<String, ChatChannelUser> listener) {
    JavaFxUtil.addListener(usersByLowerName, listener);
  }

  public void removeUserListener(MapChangeListener<String, ChatChannelUser> listener) {
    usersByLowerName.removeListener(listener);
  }
  
  public void setModerator(String username, boolean isModerator) {
    Optional.ofNullable(usersByLowerName.get(username.toLowerCase())).ifPresent(user -> user.setModerator(isModerator));
    if (isModerator) {
      moderators.add(username);
    } else {
      moderators.remove(username);
    }
  }

  public ReadOnlySetWrapper<String> getModerators() {
    return new ReadOnlySetWrapper<>(moderators);
  }

  /**
   * Returns an unmodifiable copy of the current users.
   */
  public List<ChatChannelUser> getUsers() {
    return Collections.unmodifiableList(new ArrayList<>(usersByLowerName.values()));
  }
  
  /**
   * Thread-safe way of changing user map without copying users.
   */
  public void forAllUsers(Consumer<ChatChannelUser> function) {
    synchronized (usersByLowerName) {
      for (ChatChannelUser user : usersByLowerName.values()) {
        function.accept(user);
      }
    }
  }

  public ChatChannelUser getUser(String username) {
    return usersByLowerName.get(username.toLowerCase());
  }

  public String getName() {
    return name;
  }
}
