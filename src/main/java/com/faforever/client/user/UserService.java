package com.faforever.client.user;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;

import java.util.concurrent.CompletionStage;

public interface UserService {

  BooleanProperty loggedInProperty();

  CompletionStage<Void> login(String username, String password, boolean autoLogin);

  String getUsername();

  String getPassword();

  Integer getUid();

  void cancelLogin();

  void logOut();

  ReadOnlyStringProperty currentUserProperty();
}
