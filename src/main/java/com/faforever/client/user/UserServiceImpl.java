package com.faforever.client.user;

import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.parsecom.CloudAccessor;
import com.faforever.client.preferences.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

public class UserServiceImpl implements UserService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Resource
  LobbyServerAccessor lobbyServerAccessor;
  @Resource
  PreferencesService preferencesService;
  @Resource
  CloudAccessor cloudAccessor;

  private String username;
  private String password;
  private Integer uid;

  @PostConstruct
  void postConstruct() {
    lobbyServerAccessor.addOnLoggedInListener(loginInfo -> uid = loginInfo.getId());
  }

  @Override
  public CompletableFuture<Void> login(String username, String password, boolean autoLogin) {
    preferencesService.getPreferences().getLogin()
        .setUsername(username)
        .setPassword(password)
        .setAutoLogin(autoLogin);
    preferencesService.storeInBackground();

    this.username = username;
    this.password = password;

    return lobbyServerAccessor.connectAndLogIn(username, password)
        .thenAccept(loginInfo -> {
          uid = loginInfo.getId();

          cloudAccessor.signUpOrLogIn(username, password, uid).thenAccept(s -> {
          });
        }).exceptionally(throwable -> {
          logger.warn("Login failed", throwable);
          return null;
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
  public Integer getUid() {
    return uid;
  }

  @Override
  public void cancelLogin() {
    lobbyServerAccessor.disconnect();
  }

  @Override
  public void logOut() {
    lobbyServerAccessor.disconnect();
  }
}
