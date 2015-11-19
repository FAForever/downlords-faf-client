package com.faforever.client.user;

import java.util.concurrent.CompletableFuture;

public interface UserService {

  CompletableFuture<Void> login(String username, String password, boolean autoLogin);

  String getUsername();

  String getPassword();

  Integer getUid();

  void cancelLogin();

}
