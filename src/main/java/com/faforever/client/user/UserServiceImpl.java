package com.faforever.client.user;

import com.faforever.client.legacy.ServerAccessor;
import com.faforever.client.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;

public class UserServiceImpl implements UserService {

  @Autowired
  ServerAccessor serverAccessor;

  @Override
  public boolean isLoggedIn() {
    return false;
  }

  @Override
  public void login(String username, String password, boolean autoLogin, Callback<Void> callback) {
    serverAccessor.login(username, password, callback);
  }
}
