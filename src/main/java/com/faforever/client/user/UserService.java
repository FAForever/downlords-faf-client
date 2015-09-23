package com.faforever.client.user;

import java.util.concurrent.CompletableFuture;

public interface UserService {

  CompletableFuture<Void> login(String username, String password, boolean autoLogin);

  String getUsername();

  String getPassword();

  int getUid();

  String getSessionId();

  void cancelLogin();

  String getEmail();
}
