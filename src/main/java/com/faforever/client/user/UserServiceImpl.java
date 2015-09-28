package com.faforever.client.user;

import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.preferences.PreferencesService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletableFuture;

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
  public CompletableFuture<Void> login(String username, String password, boolean autoLogin) {
    preferencesService.getPreferences().getLogin()
        .setUsername(username)
        .setPassword(password)
        .setAutoLogin(autoLogin);

    this.username = username;
    this.password = password;

    preferencesService.storeInBackground();

    return lobbyServerAccessor.connectAndLogInInBackground()
        .thenAccept(sessionInfo -> {
          UserServiceImpl.this.uid = sessionInfo.getId();
          UserServiceImpl.this.sessionId = sessionInfo.getSession();
          UserServiceImpl.this.email = sessionInfo.getEmail();
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
