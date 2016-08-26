package com.faforever.client.player;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.chat.SocialStatus;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GameStatus;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.Player;
import com.faforever.client.remote.domain.PlayersMessage;
import com.faforever.client.remote.domain.SocialMessage;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Assert;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.faforever.client.chat.SocialStatus.FOE;
import static com.faforever.client.chat.SocialStatus.FRIEND;
import static com.faforever.client.chat.SocialStatus.OTHER;
import static com.faforever.client.chat.SocialStatus.SELF;

public class PlayerServiceImpl implements PlayerService {

  private final ObservableMap<String, PlayerInfoBean> playersByName;
  private final ObservableMap<Integer, PlayerInfoBean> playersById;
  private final List<Integer> foeList;
  private final List<Integer> friendList;
  private final ObjectProperty<PlayerInfoBean> currentPlayer;
  @Resource
  FafService fafService;
  @Resource
  UserService userService;
  @Resource
  GameService gameService;
  @Resource
  EventBus eventBus;
  /**
   * Maps game IDs to status change listeners.
   */
  private Map<Integer, InvalidationListener> statusChangeListeners;

  public PlayerServiceImpl() {
    playersByName = FXCollections.observableHashMap();
    playersById = FXCollections.observableHashMap();
    friendList = new ArrayList<>();
    foeList = new ArrayList<>();
    currentPlayer = new SimpleObjectProperty<>();
    statusChangeListeners = new HashMap<>();
  }

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
    fafService.addOnMessageListener(PlayersMessage.class, this::onPlayersInfo);
    fafService.addOnMessageListener(SocialMessage.class, this::onFoeList);

    gameService.addOnGameInfoBeansChangeListener(listChange -> {
      while (listChange.next()) {
        for (GameInfoBean gameInfoBean : listChange.getRemoved()) {
          updateGameStateForPlayers(gameInfoBean);
          gameInfoBean.statusProperty().removeListener(statusChangeListeners.remove(gameInfoBean.getUid()));
        }

        for (GameInfoBean gameInfoBean : listChange.getAddedSubList()) {
          updateGameStateForPlayers(gameInfoBean);
          InvalidationListener statusChangeListener = statusChange -> updateGameStateForPlayers(gameInfoBean);
          statusChangeListeners.put(gameInfoBean.getUid(), statusChangeListener);
          gameInfoBean.statusProperty().addListener(new WeakInvalidationListener(statusChangeListener));
        }
      }
    });
  }

  @Subscribe
  public void onLoginSuccess(LoginSuccessEvent event) {
    synchronized (currentPlayer) {
      currentPlayer.set(createAndGetPlayerForUsername(event.getUsername()));
    }
  }


  private void updateGameStateForPlayers(GameInfoBean gameInfoBean) {
    ObservableMap<String, List<String>> teams = gameInfoBean.getTeams();
    synchronized (teams) {
      teams.forEach((team, players) -> updateGameStateForPlayer(players, gameInfoBean));
    }
  }

  //FIXME ugly fix until host can be resolved from gamestate
  private void updateGameStateForPlayer(List<String> players, GameInfoBean gameInfoBean) {
    for (String player : players) {
      PlayerInfoBean playerInfoBean = getPlayerForUsername(player);
      if (playerInfoBean == null) {
        continue;
      }
      updatePlayerGameStatus(playerInfoBean, GameStatus.fromGameState(gameInfoBean.getStatus()));
      playerInfoBean.setGameUid(gameInfoBean.getUid());
    }
    if (GameStatus.fromGameState(gameInfoBean.getStatus()) == GameStatus.LOBBY) {
      PlayerInfoBean host = getPlayerForUsername(gameInfoBean.getHost());
      updatePlayerGameStatus(host, GameStatus.HOST);
    }
  }

  @Override
  public void updatePlayerGameStatus(PlayerInfoBean playerInfoBean, GameStatus gameStatus) {
    if (playerInfoBean != null && playerInfoBean.getGameStatus() != gameStatus) {
      //FIXME until api, host is set twice or ugly code, I chose to set twice
      playerInfoBean.setGameStatus(gameStatus);
    }
  }

  @Override
  public PlayerInfoBean getPlayerForUsername(String username) {
    return playersByName.get(username);
  }

  @Override
  public PlayerInfoBean createAndGetPlayerForUsername(@NotNull String username) {
    Assert.checkNullArgument(username, "username must not be null");

    synchronized (playersByName) {
      if (!playersByName.containsKey(username)) {
        PlayerInfoBean player = new PlayerInfoBean(username);
        player.idProperty().addListener((observable, oldValue, newValue) -> {
          synchronized (playersById) {
            playersById.remove(oldValue.intValue());
            playersById.put(newValue.intValue(), player);
          }
        });
        playersByName.put(username, player);
      }
    }

    return playersByName.get(username);
  }

  @Override
  public Set<String> getPlayerNames() {
    return playersByName.keySet();
  }

  @Override
  public void addFriend(PlayerInfoBean player) {
    playersByName.get(player.getUsername()).setSocialStatus(FRIEND);
    friendList.add(player.getId());
    foeList.remove((Integer) player.getId());

    fafService.addFriend(player);
  }

  @Override
  public void removeFriend(PlayerInfoBean player) {
    playersByName.get(player.getUsername()).setSocialStatus(OTHER);
    friendList.remove((Integer) player.getId());

    fafService.removeFriend(player);
  }

  @Override
  public void addFoe(PlayerInfoBean player) {
    playersByName.get(player.getUsername()).setSocialStatus(FOE);
    foeList.add(player.getId());
    friendList.remove((Integer) player.getId());

    fafService.addFoe(player);
  }

  @Override
  public void removeFoe(PlayerInfoBean player) {
    playersByName.get(player.getUsername()).setSocialStatus(OTHER);
    foeList.remove((Integer) player.getId());

    fafService.removeFoe(player);
  }

  @Override
  public PlayerInfoBean getCurrentPlayer() {
    synchronized (currentPlayer) {
      if (currentPlayer.get() == null) {
        throw new IllegalStateException("Current player has not yet been set");
      }
      return currentPlayer.get();
    }
  }

  @Override
  public ReadOnlyObjectProperty<PlayerInfoBean> currentPlayerProperty() {
    return currentPlayer;
  }

  private void onPlayersInfo(PlayersMessage playersMessage) {
    playersMessage.getPlayers().forEach(this::onPlayerInfo);
  }

  private void onFoeList(SocialMessage socialMessage) {
    onFoeList(socialMessage.getFoes());
    onFriendList(socialMessage.getFriends());
  }

  private void onFoeList(List<Integer> foes) {
    updateSocialList(foeList, foes, FOE);
  }

  private void onFriendList(List<Integer> friends) {
    updateSocialList(friendList, friends, FRIEND);
  }

  private void updateSocialList(List<Integer> socialList, List<Integer> newValues, SocialStatus socialStatus) {
    socialList.clear();
    socialList.addAll(newValues);

    synchronized (playersById) {
      for (Integer userId : socialList) {
        PlayerInfoBean playerInfoBean = playersById.get(userId);
        if (playerInfoBean != null) {
          playerInfoBean.setSocialStatus(socialStatus);
        }
      }
    }
  }

  private void onPlayerInfo(Player player) {
    if (player.getLogin().equalsIgnoreCase(userService.getUsername())) {
      PlayerInfoBean playerInfoBean = getCurrentPlayer();
      playerInfoBean.updateFromPlayerInfo(player);
      playerInfoBean.setSocialStatus(SELF);
    } else {
      PlayerInfoBean playerInfoBean = createAndGetPlayerForUsername(player.getLogin());

      if (friendList.contains(player.getId())) {
        playerInfoBean.setSocialStatus(FRIEND);
      } else if (foeList.contains(player.getId())) {
        playerInfoBean.setSocialStatus(FOE);
      } else {
        playerInfoBean.setSocialStatus(OTHER);
      }

      playerInfoBean.updateFromPlayerInfo(player);
    }
  }
}
