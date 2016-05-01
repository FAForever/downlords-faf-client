package com.faforever.client.player;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GameStatus;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.GameState;
import com.faforever.client.remote.domain.Player;
import com.faforever.client.remote.domain.PlayersMessage;
import com.faforever.client.remote.domain.SocialMessage;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Assert;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.faforever.client.chat.SocialStatus.FOE;
import static com.faforever.client.chat.SocialStatus.FRIEND;
import static com.faforever.client.chat.SocialStatus.OTHER;
import static com.faforever.client.chat.SocialStatus.SELF;

public class PlayerServiceImpl implements PlayerService {

  private static final Lock CURRENT_PLAYER_LOCK = new ReentrantLock();

  private final ObservableMap<String, PlayerInfoBean> playersByName;
  private final ObservableMap<Integer, PlayerInfoBean> playersById;
  private final List<Integer> foeList;
  private final List<Integer> friendList;

  @Resource
  FafService fafService;
  @Resource
  UserService userService;
  @Resource
  GameService gameService;
  private ObjectProperty<PlayerInfoBean> currentPlayer;

  public PlayerServiceImpl() {
    playersByName = FXCollections.observableHashMap();
    playersById = FXCollections.observableHashMap();
    friendList = new ArrayList<>();
    foeList = new ArrayList<>();
    currentPlayer = new SimpleObjectProperty<>();
  }

  @PostConstruct
  void init() {
    fafService.addOnMessageListener(PlayersMessage.class, this::onPlayersInfo);
    fafService.addOnMessageListener(SocialMessage.class, this::onFoeList);
    gameService.addOnGameInfoBeansChangeListener(change -> {
      while (change.next()) {
        for (GameInfoBean gameInfoBean : change.getRemoved()) {
          gameInfoBean.setStatus(GameState.CLOSED);
          gameInfoBean.getTeams().forEach((team, players) -> updatePlayerInfoBean(players, gameInfoBean));
        }
        for (GameInfoBean gameInfoBean : change.getAddedSubList()) {
          gameInfoBean.getTeams().forEach((team, players) -> updatePlayerInfoBean(players, gameInfoBean));
          gameInfoBean.statusProperty().addListener(change2 -> {
            gameInfoBean.getTeams().forEach((team, updatedPlayer) -> updatePlayerInfoBean(updatedPlayer, gameInfoBean));
          });
        }
      }
    });
  }

  //FIXME ugly fix until host can be resolved from gamestate
  private void updatePlayerInfoBean(List<String> players, GameInfoBean gameInfoBean) {
    for (String player : players) {
      PlayerInfoBean playerInfoBean = getPlayerForUsername(player);
      if (playerInfoBean == null) {
        continue;
      }
      updatePlayerGameStatus(playerInfoBean, GameStatus.getFromGameState(gameInfoBean.getStatus()));
      playerInfoBean.setGameUid(gameInfoBean.getUid());
    }
    if (GameStatus.getFromGameState(gameInfoBean.getStatus()) == GameStatus.LOBBY) {
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
    Assert.checkNull(username, "username must not be null");

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
    CURRENT_PLAYER_LOCK.lock();
    try {
      if (currentPlayer.get() == null) {
        currentPlayer.set(createAndGetPlayerForUsername(userService.getUsername()));
      }
    } finally {
      CURRENT_PLAYER_LOCK.unlock();
    }
    return currentPlayer.get();
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
    foeList.clear();
    foeList.addAll(foes);

    synchronized (playersById) {
      for (Integer foeId : foes) {
        PlayerInfoBean playerInfoBean = playersById.get(foeId);
        if (playerInfoBean != null) {
          playerInfoBean.setSocialStatus(FOE);
        }
      }
    }
  }

  private void onFriendList(List<Integer> friends) {
    friendList.clear();
    friendList.addAll(friends);

    synchronized (playersById) {
      for (Integer friendId : friendList) {
        PlayerInfoBean playerInfoBean = playersById.get(friendId);
        if (playerInfoBean != null) {
          playerInfoBean.setSocialStatus(FRIEND);
        }
      }
    }
  }

  private void onPlayerInfo(Player player) {
    if (player.getLogin().equals(userService.getUsername())) {
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
