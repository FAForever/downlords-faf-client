package com.faforever.client.game;

import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.mapstruct.GameMapper;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.commons.lobby.GameInfo;
import com.faforever.commons.lobby.GameStatus;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.Optional;
import java.util.Set;

/**
 * Downloads necessary maps, mods and updates before starting
 */
@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class GameService implements InitializingBean {

  private final FafServerAccessor fafServerAccessor;
  private final PlayerService playerService;
  private final GameMapper gameMapper;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final ObservableMap<Integer, GameBean> gameIdToGame = FXCollections.synchronizedObservableMap(
      FXCollections.observableHashMap());
  @Getter
  private final ObservableList<GameBean> games = JavaFxUtil.attachListToMap(FXCollections.synchronizedObservableList(
                                                                                FXCollections.observableArrayList(
                                                                                    game -> new Observable[]{game.statusProperty(), game.teamsProperty(), game.titleProperty(), game.mapFolderNameProperty(), game.simModsProperty(), game.passwordProtectedProperty()})),
                                                                            gameIdToGame);

  @Override
  public void afterPropertiesSet() {
    fafServerAccessor.getEvents(GameInfo.class)
                     .flatMap(gameInfo -> gameInfo.getGames() == null ? Flux.just(
                                                         gameInfo) : Flux.fromIterable(gameInfo.getGames()))
                     .flatMap(gameInfo -> Mono.zip(Mono.just(gameInfo), Mono.justOrEmpty(getByUid(
                                                                                           gameInfo.getUid()))
                                                                                       .switchIfEmpty(
                                                                                           initializeGameBean(
                                                                                               gameInfo))))
                     .publishOn(fxApplicationThreadExecutor.asScheduler())
                     .map(TupleUtils.function(gameMapper::update))
                     .doOnError(throwable -> log.error("Error processing game", throwable))
                     .filter(game -> game.getStatus() == GameStatus.CLOSED)
                     .doOnNext(GameBean::removeListeners)
                     .map(GameBean::getId)
                     .publishOn(fxApplicationThreadExecutor.asScheduler())
                     .doOnNext(gameIdToGame::remove)
                     .doOnError(throwable -> log.error("Error closing game", throwable))
                     .retry()
                     .subscribe();

    fafServerAccessor.connectionStateProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == ConnectionState.DISCONNECTED) {
        fxApplicationThreadExecutor.execute(gameIdToGame::clear);
      }
    });
  }

  private Mono<GameBean> initializeGameBean(GameInfo gameInfo) {
    return Mono.fromCallable(() -> {
                 GameBean newGame = new GameBean();
                 newGame.setId(gameInfo.getUid());
                 newGame.addPlayerChangeListener(generatePlayerChangeListener(newGame));
                 return newGame;
               }).doOnNext(game -> gameMapper.update(gameInfo, game))
               .publishOn(fxApplicationThreadExecutor.asScheduler())
               .doOnNext(game -> gameIdToGame.put(game.getId(), game));
  }

  private ChangeListener<Set<Integer>> generatePlayerChangeListener(GameBean newGame) {
    return (observable, oldValue, newValue) -> {
      oldValue.stream()
              .filter(player -> !newValue.contains(player))
              .map(playerService::getPlayerByIdIfOnline)
              .flatMap(Optional::stream)
              .filter(player -> newGame.equals(player.getGame()))
              .forEach(player -> player.setGame(null));

      newValue.stream()
              .filter(player -> !oldValue.contains(player))
              .map(playerService::getPlayerByIdIfOnline)
              .flatMap(Optional::stream)
              .forEach(player -> player.setGame(newGame));
    };
  }

  public Optional<GameBean> getByUid(Integer uid) {
    return Optional.ofNullable(gameIdToGame.get(uid));
  }
}
