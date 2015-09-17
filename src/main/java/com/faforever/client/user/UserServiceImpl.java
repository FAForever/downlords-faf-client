package com.faforever.client.user;

import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.domain.SessionInfo;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;

public class UserServiceImpl implements UserService {

  @Autowired
  LobbyServerAccessor lobbyServerAccessor;

  @Autowired
  PreferencesService preferencesService;

  private String username;
  private String password;
  private int uid;
  private String sessionId;
  private String email;

  @Override
  public void login(String username, String password, boolean autoLogin, Callback<Void> callback) {
    preferencesService.getPreferences().getLogin()
        .setUsername(username)
        .setPassword(password)
        .setAutoLogin(autoLogin);

    this.username = username;
    this.password = password;

    preferencesService.storeInBackground();
    lobbyServerAccessor.connectAndLogInInBackground(new Callback<SessionInfo>() {

      @Override
      public void success(SessionInfo result) {
        UserServiceImpl.this.uid = result.getId();
        UserServiceImpl.this.sessionId = result.getSession();
        UserServiceImpl.this.email = result.getEmail();
        callback.success(null);
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

  @Override
  public int getUid() {
    return uid;
  }

  @Override
  public String getSessionId() {
    return sessionId;
  }

  @Override
  public void cancelLogin() {
    lobbyServerAccessor.disconnect();
  }

  @Override
  public String getEmail() {
    return email;
  }
}
