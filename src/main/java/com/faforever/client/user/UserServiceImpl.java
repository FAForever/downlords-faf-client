package com.faforever.client.user;

import com.faforever.client.legacy.ServerAccessor;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;

public class UserServiceImpl implements UserService {

  @Autowired
  ServerAccessor serverAccessor;

  @Autowired
  PreferencesService preferencesService;

  private boolean isLoggedIn;
  private String username;
  private String password;

  @Override
  public boolean isLoggedIn() {
    return isLoggedIn;
  }

  @Override
  public void login(final String username, final String password, final boolean autoLogin, final Callback<Void> callback) {
    serverAccessor.login(username, password, new Callback<Void>() {
      @Override
      public void success(Void result) {
        if (autoLogin) {
          preferencesService.getPreferences().getLoginPrefs()
              .setUsername(username)
              .setPassword(password);
          preferencesService.store();
        }

        isLoggedIn = true;
        UserServiceImpl.this.username = username;
        UserServiceImpl.this.password = password;
        callback.success(result);
      }

      @Override
      public void error(Throwable e) {
        callback.error(e);
      }
    });
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getPassword() {
    return password;
  }
}
