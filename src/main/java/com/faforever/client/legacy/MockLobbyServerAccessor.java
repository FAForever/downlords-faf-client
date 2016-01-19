package com.faforever.client.legacy;

import com.faforever.client.game.Faction;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.GameType;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameInfoMessage;
import com.faforever.client.legacy.domain.GameLaunchMessage;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.GameTypeMessage;
import com.faforever.client.legacy.domain.LoginMessage;
import com.faforever.client.legacy.domain.Player;
import com.faforever.client.legacy.domain.PlayersMessage;
import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.rankedmatch.MatchmakerMessage;
import com.faforever.client.relay.GpgClientMessage;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.user.UserService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.faforever.client.legacy.domain.GameAccess.PASSWORD;
import static com.faforever.client.legacy.domain.GameAccess.PUBLIC;
import static com.faforever.client.task.AbstractPrioritizedTask.Priority.HIGH;

public class MockLobbyServerAccessor implements LobbyServerAccessor {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Timer timer;
  private final HashMap<Class<? extends ServerMessage>, Collection<Consumer<ServerMessage>>> messageListeners;
  @Resource
  UserService userService;
  @Resource
  TaskService taskService;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  private ObjectProperty<ConnectionState> connectionState;

  public MockLobbyServerAccessor() {
    timer = new Timer("LobbyServerAccessorTimer", true);
    messageListeners = new HashMap<>();
    connectionState = new SimpleObjectProperty<>();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener) {
    if (!messageListeners.containsKey(type)) {
      messageListeners.put(type, new LinkedList<>());
    }
    messageListeners.get(type).add((Consumer<ServerMessage>) listener);
  }

  @Override
  public <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener) {
    messageListeners.get(type).remove(listener);
  }

  @Override
  public ObjectProperty<ConnectionState> connectionStateProperty() {
    return connectionState;
  }

  @Override
  public CompletableFuture<LoginMessage> connectAndLogIn(String username, String password) {
    return taskService.submitTask(new AbstractPrioritizedTask<LoginMessage>(HIGH) {
      @Override
      protected LoginMessage call() throws Exception {
        updateTitle(i18n.get("login.progress.message"));

        GameTypeMessage gameTypeMessage = new GameTypeMessage();
        gameTypeMessage.setFullname("Forged Alliance Forever");
        gameTypeMessage.setName("faf");
        gameTypeMessage.setLive(true);
        gameTypeMessage.setHost(true);
        gameTypeMessage.setDesc("Description");

        messageListeners.getOrDefault(gameTypeMessage.getClass(), Collections.emptyList()).forEach(consumer -> consumer.accept(gameTypeMessage));

        Player player = new Player();
        player.setLogin(userService.getUsername());
        player.setClan("ABC");
        player.setCountry("A1");
        player.setRatingMean(1500);
        player.setRatingDeviation(220);
        player.setLadderRatingMean(1500);
        player.setLadderRatingDeviation(220);
        player.setNumberOfGames(330);

        PlayersMessage playersMessage = new PlayersMessage();
        playersMessage.setPlayers(Collections.singletonList(player));

        messageListeners.getOrDefault(playersMessage.getClass(), Collections.emptyList()).forEach(consumer -> consumer.accept(playersMessage));

        timer.schedule(new TimerTask() {
          @Override
          public void run() {
            MatchmakerMessage matchmakerServerMessage = new MatchmakerMessage();
            matchmakerServerMessage.setPotential(true);
            messageListeners.getOrDefault(matchmakerServerMessage.getClass(), Collections.emptyList()).forEach(consumer -> consumer.accept(matchmakerServerMessage));
          }
        }, 7000);


        List<GameInfoMessage> gameInfoMessages = Arrays.asList(
            createGameInfo(1, "Mock game 500 - 800", PUBLIC, "faf", "scmp_010", 3, 6, "Mock user"),
            createGameInfo(2, "Mock game 500+", PUBLIC, "faf", "scmp_011", 3, 6, "Mock user"),
            createGameInfo(3, "Mock game +500", PUBLIC, "faf", "scmp_012", 3, 6, "Mock user"),
            createGameInfo(4, "Mock game <1000", PUBLIC, "faf", "scmp_013", 3, 6, "Mock user"),
            createGameInfo(5, "Mock game >1000", PUBLIC, "faf", "scmp_014", 3, 6, "Mock user"),
            createGameInfo(6, "Mock game ~600", PASSWORD, "faf", "scmp_015", 3, 6, "Mock user"),
            createGameInfo(7, "Mock game 7", PASSWORD, "faf", "scmp_016", 3, 6, "Mock user")
        );

        gameInfoMessages.forEach(gameInfoMessage ->
            messageListeners.getOrDefault(gameInfoMessage.getClass(), Collections.emptyList())
                .forEach(consumer -> consumer.accept(gameInfoMessage)));

        notificationService.addNotification(
            new PersistentNotification(
                "How about a long-running (7s) mock task?",
                Severity.INFO,
                Arrays.asList(
                    new Action("Execute", event ->
                        taskService.submitTask(new AbstractPrioritizedTask<Void>(HIGH) {
                          @Override
                          protected Void call() throws Exception {
                            updateTitle("Mock task");
                            Thread.sleep(2000);
                            for (int i = 0; i < 5; i++) {
                              updateProgress(i, 5);
                              Thread.sleep(1000);
                            }
                            return null;
                          }
                        })),
                    new Action("Nope")
                )
            )
        );

        LoginMessage sessionInfo = new LoginMessage();
        sessionInfo.setId(123);
        sessionInfo.setLogin("MockUser");
        return sessionInfo;
      }
    });
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestNewGame(NewGameInfo newGameInfo) {
    return taskService.submitTask(new AbstractPrioritizedTask<GameLaunchMessage>(HIGH) {
      @Override
      protected GameLaunchMessage call() throws Exception {
        updateTitle(i18n.get("requestNewGameTask.title"));

        GameLaunchMessage gameLaunchMessage = new GameLaunchMessage();
        gameLaunchMessage.setArgs(Arrays.asList("/ratingcolor d8d8d8d8", "/numgames 1234"));
        gameLaunchMessage.setMod("faf");
        gameLaunchMessage.setUid(1234);
        return gameLaunchMessage;
      }
    });
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestJoinGame(GameInfoBean gameInfoBean, String password) {
    return taskService.submitTask(new AbstractPrioritizedTask<GameLaunchMessage>(HIGH) {
      @Override
      protected GameLaunchMessage call() throws Exception {
        updateTitle(i18n.get("requestJoinGameTask.title"));

        GameLaunchMessage gameLaunchMessage = new GameLaunchMessage();
        gameLaunchMessage.setArgs(Arrays.asList("/ratingcolor d8d8d8d8", "/numgames 1234"));
        gameLaunchMessage.setMod("faf");
        gameLaunchMessage.setUid(1234);
        return gameLaunchMessage;
      }
    });
  }

  @Override
  public void disconnect() {

  }

  @Override
  public void setFriends(Collection<String> friends) {

  }

  @Override
  public void setFoes(Collection<String> foes) {

  }

  @Override
  public CompletableFuture<GameLaunchMessage> startSearchRanked1v1(Faction faction, int gamePort) {
    logger.debug("Searching 1v1 match with faction: {}", faction);
    GameLaunchMessage gameLaunchMessage = new GameLaunchMessage();
    gameLaunchMessage.setUid(123);
    gameLaunchMessage.setMod(GameType.DEFAULT.getString());
    return CompletableFuture.completedFuture(gameLaunchMessage);
  }

  @Override
  public void stopSearchingRanked() {
    logger.debug("Stopping searching 1v1 match");
  }

  @Override
  public void expand1v1Search(float radius) {
  }

  @Override
  public Long getSessionId() {
    return null;
  }

  @Override
  public void sendGpgMessage(GpgClientMessage message) {

  }

  @Override
  public void initConnectivityTest(int port) {

  }

  private GameInfoMessage createGameInfo(int uid, String title, GameAccess access, String featuredMod, String mapName, int numPlayers, int maxPlayers, String host) {
    GameInfoMessage gameInfoMessage = new GameInfoMessage();
    gameInfoMessage.setUid(uid);
    gameInfoMessage.setTitle(title);
    gameInfoMessage.setFeaturedMod(featuredMod);
    gameInfoMessage.setMapname(mapName);
    gameInfoMessage.setNumPlayers(numPlayers);
    gameInfoMessage.setMaxPlayers(maxPlayers);
    gameInfoMessage.setHost(host);
    gameInfoMessage.setState(GameState.OPEN);
    gameInfoMessage.setSimMods(Collections.emptyMap());
    gameInfoMessage.setTeams(Collections.emptyMap());
    gameInfoMessage.setFeaturedModVersions(Collections.emptyMap());

    return gameInfoMessage;
  }
}
