package com.faforever.client.user;

import com.faforever.client.util.Callback;

public interface UserService {

  void login(String username, String password, boolean autoLogin, Callback<Void> callback);

  String getUsername();

  String getPassword();

  int getUid();

  int getSessionId();

  void cancelLogin();
}
