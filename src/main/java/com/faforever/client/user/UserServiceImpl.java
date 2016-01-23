package com.faforever.client.user;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.legacy.domain.LoginMessage;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class UserServiceImpl implements UserService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Resource
  FafService fafService;
  @Resource
  PreferencesService preferencesService;
  @Resource
  FafApiAccessor fafApiAccessor;

  private String username;
  private String password;
  private Integer uid;
  private Collection<Runnable> onLogoutListeners;
  private Collection<Runnable> onLoginListeners;

  public UserServiceImpl() {
    onLogoutListeners = new ArrayList<>();
    onLoginListeners = new ArrayList<>();
  }

  @PostConstruct
  void postConstruct() {
    fafService.addOnMessageListener(LoginMessage.class, loginInfo -> uid = loginInfo.getId());
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

    return fafService.connectAndLogIn(username, password)
        .thenAccept(loginInfo -> {
          uid = loginInfo.getId();

          fafApiAccessor.authorize(loginInfo.getId());
          onLoginListeners.forEach(Runnable::run);
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
    fafService.disconnect();
  }

  @Override
  public void logOut() {
    logger.info("Logging out");
    fafService.disconnect();
    onLogoutListeners.forEach(Runnable::run);
  }

  @Override
  public void addOnLogoutListener(Runnable listener) {
    onLogoutListeners.add(listener);
  }

  @Override
  public void addOnLoginListener(Runnable listener) {
    onLoginListeners.add(listener);
  }
}
