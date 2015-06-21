package com.faforever.client.user;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

public class UserServiceImpl implements UserService {

  @Autowired
  LobbyServerAccessor lobbyServerAccessor;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  PlayerService playerService;

  private String username;
  private String password;
  private PlayerInfoBean currentPlayer;

  @PostConstruct
  void init() {
    playerService.addPlayerListener(change -> {
      if (change.wasRemoved()) {
        return;
      }

      PlayerInfoBean playerInfoBean = change.getValueAdded();
      if (username.equals(playerInfoBean.getUsername())) {
        this.currentPlayer = playerInfoBean;
      }
    });
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
    lobbyServerAccessor.connectAndLogInInBackground(callback);
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
  public PlayerInfoBean getCurrentPlayer() {
    return currentPlayer;
  }


  @Override
  public void cancelLogin() {
    lobbyServerAccessor.disconnect();
  }
}
