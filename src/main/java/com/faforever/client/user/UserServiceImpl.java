package com.faforever.client.user;

import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.domain.SessionInfo;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;

public class UserServiceImpl implements UserService {

  @Autowired
  LobbyServerAccessor lobbyServerAccessor;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  PlayerService playerService;

  private String username;
  private String password;
  private int uid;
  private String sessionId;

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
        UserServiceImpl.this.uid = result.id;
        UserServiceImpl.this.sessionId = result.session;
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
}
