package com.faforever.client.user;

import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.parsecom.CloudAccessor;
import com.faforever.client.play.PlayServices;
import com.faforever.client.preferences.PreferencesService;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  @Resource
  PlayServices playServices;

  private String username;
  private String password;
  private int uid;
  private String sessionId;
  private StringProperty email;

  public UserServiceImpl() {
    email = new SimpleStringProperty();
  }

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
          UserServiceImpl.this.email.set(sessionInfo.getEmail());

          cloudAccessor.signUpOrLogIn(username, password, uid).thenAccept(s -> {
            if (preferencesService.getPreferences().getConnectedToGooglePlay()) {
              playServices.authorize();
            }
          }).exceptionally(throwable -> {
            logger.warn("Login to cloud services failed", throwable);
            return null;
          });
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
    return email.get();
  }

  @Override
  public ReadOnlyStringProperty emailProperty() {
    return email;
  }
}
