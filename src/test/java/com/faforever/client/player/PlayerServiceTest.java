package com.faforever.client.player;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.GameInfoBuilder;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mapstruct.PlayerMapper;
import com.faforever.client.preferences.UserPrefs;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.user.LoginService;
import com.faforever.commons.api.elide.ElideEntity;
import com.faforever.commons.lobby.Player;
import com.faforever.commons.lobby.Player.Avatar;
import com.faforever.commons.lobby.Player.LeaderboardStats;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerServiceTest extends ServiceTest {

  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private LoginService loginService;

  @Mock
  private FxApplicationThreadExecutor fxApplicationThreadExecutor;
  @Spy
  private PlayerMapper playerMapper = Mappers.getMapper(PlayerMapper.class);
  @Spy
  private UserPrefs userPrefs;

  @InjectMocks
  private PlayerService instance;
  private com.faforever.commons.lobby.Player currentPlayer;
  private com.faforever.commons.lobby.Player playerInfo1;
  private com.faforever.commons.lobby.Player playerInfo2;

  private final TestPublisher<com.faforever.commons.lobby.PlayerInfo> playerInfoTestPublisher = TestPublisher.create();

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(playerMapper);
    lenient().when(fxApplicationThreadExecutor.asScheduler()).thenReturn(Schedulers.immediate());
    lenient().when(fafServerAccessor.getEvents(com.faforever.commons.lobby.PlayerInfo.class))
             .thenReturn(playerInfoTestPublisher.flux());
    currentPlayer = new Player(1, "junit", null, null, "", new HashMap<>(), new HashMap<>(), null);
    playerInfo1 = new com.faforever.commons.lobby.Player(2, "junit2", null,
                                                         new Avatar("https://test.com/test.png", "junit"), "",
                                                         new HashMap<>(), new HashMap<>(), null);
    playerInfo2 = new com.faforever.commons.lobby.Player(3, "junit3", null, null, "", new HashMap<>(), new HashMap<>(),
                                                         null);

    lenient().when(loginService.ownPlayerProperty()).thenReturn(new ReadOnlyObjectWrapper<>(currentPlayer));
    lenient().when(loginService.getUsername()).thenReturn("junit");
    lenient().when(loginService.getUserId()).thenReturn(1);

    userPrefs.getNotesByPlayerId().put(3, "junit3");

    lenient().when(loginService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>());

    instance.afterPropertiesSet();
    playerInfoTestPublisher.next(new com.faforever.commons.lobby.PlayerInfo(List.of(playerInfo1, playerInfo2)));

    playerInfoTestPublisher.assertSubscribers(1);
  }

  @Test
  public void testPlayerComesOnline() {
    playerInfoTestPublisher.next(new com.faforever.commons.lobby.PlayerInfo(List.of(
        new com.faforever.commons.lobby.Player(4, "junit3", null, null, "", new HashMap<>(), new HashMap<>(), null))));

    assertTrue(instance.getPlayerByIdIfOnline(4).isPresent());
  }

  @Test
  public void testGetPlayerForUsernameUsernameDoesNotExist() {
    Optional<PlayerInfo> player = instance.getPlayerByNameIfOnline("test");
    assertFalse(player.isPresent());
  }

  @Test
  public void testGetPlayerForUsernameUsernameExists() {
    PlayerInfo player = instance.getPlayerByNameIfOnline("junit2").orElseThrow();

    assertEquals("junit2", player.getUsername());
  }

  @Test
  public void testPlayerUpdatedFromPlayerInfo() {
    PlayerInfo player = instance.getPlayerByNameIfOnline(playerInfo1.getLogin()).orElseThrow();

    assertEquals(playerInfo1.getRatings().values().stream().mapToInt(LeaderboardStats::getNumberOfGames).sum(), player.getNumberOfGames());
    assertEquals(playerInfo1.getClan(), player.getClan());
    assertEquals(playerInfo1.getCountry(), player.getCountry());

    playerInfoTestPublisher.next(new com.faforever.commons.lobby.PlayerInfo(List.of(
        new com.faforever.commons.lobby.Player(2, "junit2", "ABC", null, "DE", new HashMap<>(), new HashMap<>(),
                                               null))));

    assertEquals(0, player.getNumberOfGames());
    assertEquals("ABC", player.getClan());
    assertEquals("DE", player.getCountry());
  }

  @Test
  public void testGetPlayerNamesPopulated() {
    Set<String> playerNames = instance.getPlayerNames();
    assertThat(playerNames, hasSize(3));
  }

  @Test
  public void testGetPlayerNamesSomeInstances() {
    Set<String> playerNames = instance.getPlayerNames();

    assertThat(playerNames, hasSize(3));
    assertThat(playerNames, containsInAnyOrder(currentPlayer.getLogin(), playerInfo1.getLogin(), playerInfo2.getLogin()));
  }

  @Test
  public void testGetCurrentPlayer() {
    PlayerInfo currentPlayer = instance.getCurrentPlayer();

    assertThat(currentPlayer.getUsername(), is("junit"));
    assertThat(currentPlayer.getId(), is(1));
  }

  @Test
  public void testGetPlayerByName() {
    PlayerInfo playerInfo = PlayerInfoBuilder.create().defaultValues().get();
    Flux<ElideEntity> resultFlux = Flux.just(playerMapper.map(playerInfo));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    StepVerifier.create(instance.getPlayerByName("test")).expectNext(playerInfo).verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().string("login").eq("test"))));
  }

  @Test
  public void testGetPlayerByNamePlayerOnline() {
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());

    StepVerifier.create(instance.getPlayerByName("junit2")).expectNextCount(1).verifyComplete();
  }

  @Test
  public void testGetPlayersByIds() {
    PlayerInfo playerInfo = PlayerInfoBuilder.create().defaultValues().username("junit4").id(4).get();
    Flux<ElideEntity> resultFlux = Flux.just(playerMapper.map(playerInfo));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    instance.getPlayersByIds(List.of(1, 2, 3, 4)).blockLast();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("id").in(List.of(4)))));
  }

  @Test
  public void testGetPlayersByIdsAllPlayersOnline() {
    StepVerifier.create(instance.getPlayersByIds(List.of(2, 3))).expectNextCount(2).verifyComplete();

    verify(fafApiAccessor, never()).getMany(any());
  }

  @Test
  public void testCurrentPlayerInGame() {
    GameInfo game = GameInfoBuilder.create().defaultValues().teams(Map.of(1, List.of(1))).get();

    assertTrue(instance.isCurrentPlayerInGame(game));
  }

  @Test
  public void testCurrentPlayerNotInGame() {
    GameInfo game = GameInfoBuilder.create().defaultValues().teams(Map.of(1, List.of(0))).get();

    assertFalse(instance.isCurrentPlayerInGame(game));
  }

  @Test
  public void testRemovePlayer() {
    assertFalse(instance.getPlayerNames().isEmpty());
    assertTrue(instance.isOnline(playerInfo1.getId()));
    assertTrue(instance.isOnline(playerInfo2.getId()));

    instance.getPlayerByNameIfOnline(playerInfo1.getLogin())
            .ifPresent(player -> player.setServerStatus(ServerStatus.OFFLINE));
    instance.getPlayerByNameIfOnline(playerInfo2.getLogin())
            .ifPresent(player -> player.setServerStatus(ServerStatus.OFFLINE));

    assertFalse(instance.getPlayerNames().contains(playerInfo1.getLogin()));
    assertFalse(instance.getPlayerNames().contains(playerInfo2.getLogin()));
    assertFalse(instance.isOnline(playerInfo1.getId()));
    assertFalse(instance.isOnline(playerInfo2.getId()));
  }
}
