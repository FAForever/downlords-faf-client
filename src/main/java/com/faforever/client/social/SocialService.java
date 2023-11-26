package com.faforever.client.social;

import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.player.FriendJoinedGameNotifier;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.UserPrefs;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.commons.lobby.GameType;
import com.faforever.commons.lobby.SocialInfo;
import javafx.collections.MapChangeListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static com.faforever.client.player.SocialStatus.OTHER;
import static com.faforever.client.player.SocialStatus.SELF;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialService implements InitializingBean {

  private final PlayerService playerService;
  private final FafServerAccessor fafServerAccessor;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final FriendJoinedGameNotifier friendJoinedGameNotifier;
  private final UserPrefs userPrefs;

  private final List<Integer> foeList = new ArrayList<>();
  private final List<Integer> friendList = new ArrayList<>();

  @Override
  public void afterPropertiesSet() {
    fafServerAccessor.getEvents(SocialInfo.class)
                     .flatMap(socialInfo -> {
                       Flux<PlayerBean> friendFlux = Mono.just(socialInfo.getFriends())
                                                         .doOnNext(ids -> {
                                                           friendList.clear();
                                                           friendList.addAll(ids);
                                                         })
                                                         .flatMapMany(Flux::fromIterable)
                                                         .map(playerService::getPlayerByIdIfOnline)
                                                         .flatMap(Mono::justOrEmpty);

                       Flux<PlayerBean> foeFlux = Mono.just(socialInfo.getFoes())
                                                      .doOnNext(ids -> {
                                                        foeList.clear();
                                                        foeList.addAll(ids);
                                                      })
                                                      .flatMapMany(Flux::fromIterable)
                                                      .map(playerService::getPlayerByIdIfOnline)
                                                      .flatMap(Mono::justOrEmpty);

                       return Flux.merge(friendFlux, foeFlux);
                     })
                     .publishOn(fxApplicationThreadExecutor.asScheduler())
                     .doOnNext(this::updatePlayerSocialStatus)
                     .publishOn(Schedulers.single())
                     .doOnError(throwable -> log.error("Error processing social info", throwable))
                     .retry()
                     .subscribe();

    userPrefs.getNotesByPlayerId().addListener(
        (MapChangeListener<Integer, String>) change -> playerService.getPlayerByIdIfOnline(change.getKey())
                                                                    .ifPresent(player -> {
                                                                      if (change.wasAdded()) {
                                                                        player.setNote(change.getValueAdded());
                                                                      } else if (change.wasRemoved()) {
                                                                        player.setNote(null);
                                                                      }
                                                                    }));

    playerService.addPlayerOnlineListener(player -> {
      updatePlayerSocialStatus(player);
      player.gameProperty().when(player.socialStatusProperty().isEqualTo(FRIEND)).subscribe(game -> {
        if (game != null && (game.getGameType() == GameType.CUSTOM || game.getGameType() == GameType.COOP)) {
          friendJoinedGameNotifier.onFriendJoinedGame(player, game);
        }
      });
    });
  }

  public void updateNote(PlayerBean player, String text) {
    if (StringUtils.isBlank(text)) {
      removeNote(player);
    } else {
      String normalizedText = text.replaceAll("\\n\\n+", "\n");
      userPrefs.getNotesByPlayerId().put(player.getId(), normalizedText);
    }
  }

  public void removeNote(PlayerBean player) {
    userPrefs.getNotesByPlayerId().remove(player.getId());
  }

  public void addFriend(PlayerBean player) {
    player.setSocialStatus(FRIEND);
    friendList.add(player.getId());
    foeList.remove(player.getId());
    fafServerAccessor.addFriend(player.getId());
  }

  public void removeFriend(PlayerBean player) {
    player.setSocialStatus(OTHER);
    friendList.remove(player.getId());
    fafServerAccessor.removeFriend(player.getId());
  }

  public void addFoe(PlayerBean player) {
    player.setSocialStatus(FOE);
    foeList.add(player.getId());
    friendList.remove(player.getId());
    fafServerAccessor.addFoe(player.getId());
  }

  public void removeFoe(PlayerBean player) {
    player.setSocialStatus(OTHER);
    foeList.remove(player.getId());
    fafServerAccessor.removeFoe(player.getId());
  }

  public boolean areFriendsInGame(GameBean game) {
    if (game == null) {
      return false;
    }
    return game.getAllPlayersInGame().stream().anyMatch(friendList::contains);
  }

  private void updatePlayerSocialStatus(PlayerBean player) {
    Integer id = player.getId();
    if (player.equals(playerService.getCurrentPlayer())) {
      player.setSocialStatus(SELF);
    } else if (friendList.contains(id)) {
      player.setSocialStatus(FRIEND);
    } else if (foeList.contains(id)) {
      player.setSocialStatus(FOE);
    } else {
      player.setSocialStatus(OTHER);
    }

    player.setNote(userPrefs.getNotesByPlayerId().get(id));
  }
}
