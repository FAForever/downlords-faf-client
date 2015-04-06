package com.faforever.client.user;

import com.faforever.client.legacy.message.OnPlayerInfoMessageListener;
import com.faforever.client.legacy.ServerAccessor;
import com.faforever.client.legacy.message.PlayerInfoMessage;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

public class UserServiceImpl implements UserService, OnPlayerInfoMessageListener {

  @Autowired
  ServerAccessor serverAccessor;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  PlayerService playerService;

  private boolean isLoggedIn;
  private String clan;
  private String country;
  private Float deviation;
  private Float mean;
  private String username;
  private String password;

  @PostConstruct
  void init() {
    playerService.addOnPlayerInfoListener(this);
  }

  @Override
  public boolean isLoggedIn() {
    return isLoggedIn;
  }

  @Override
  public void login(final String username, final String password, final boolean autoLogin, final Callback<Void> callback) {
    preferencesService.getPreferences().getLogin()
        .setUsername(username)
        .setPassword(password)
        .setAutoLogin(autoLogin);

    this.username = username;
    this.password = password;

    preferencesService.storeInBackground();
    serverAccessor.connectAndLogInInBackground(callback);
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
  public String getClan() {
    return clan;
  }

  @Override
  public String getCountry() {
    return country;
  }

  @Override
  public Float getDeviation() {
    return deviation;
  }

  @Override
  public Float getMean() {
    return mean;
  }

  @Override
  public void onPlayerInfoMessage(PlayerInfoMessage playerInfoMessage) {
    if (username.equals(playerInfoMessage.login)) {
      this.clan = playerInfoMessage.clan;
      this.country = playerInfoMessage.country;
      this.deviation = playerInfoMessage.ratingDeviation;
      this.mean = playerInfoMessage.ratingMean;
    }
  }
}
