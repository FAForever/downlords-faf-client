package com.faforever.client.user;

import javafx.beans.property.BooleanProperty;

import java.util.concurrent.CompletableFuture;

public interface UserService {

  BooleanProperty loggedInProperty();

  CompletableFuture<Void> login(String username, String password, boolean autoLogin);

  String getUsername();

  String getPassword();

  Integer getUid();

  void cancelLogin();

  void logOut();

}
