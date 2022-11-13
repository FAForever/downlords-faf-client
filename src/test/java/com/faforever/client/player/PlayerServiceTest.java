package com.faforever.client.player;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.avatar.AvatarService;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mapstruct.PlayerMapper;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.user.UserService;
import com.faforever.commons.api.elide.ElideEntity;
import com.faforever.commons.lobby.Player.Avatar;
import com.faforever.commons.lobby.Player.LeaderboardStats;
import com.faforever.commons.lobby.SocialInfo;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerServiceTest extends ServiceTest {

  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private UserService userService;
  @Mock
  private AvatarService avatarService;
  @Mock
  private EventBus eventBus;
  @Mock
  private PreferencesService preferencesService;

  @InjectMocks
  private PlayerService instance;
  @Spy
  private PlayerMapper playerMapper = Mappers.getMapper(PlayerMapper.class);
  private com.faforever.commons.lobby.Player playerInfo1;
  private com.faforever.commons.lobby.Player playerInfo2;

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(playerMapper);
    when(userService.getOwnPlayer()).thenReturn(new com.faforever.commons.lobby.Player(1, "junit", null, null, "", new HashMap<>(), new HashMap<>()));
    when(userService.getUsername()).thenReturn("junit");
    playerInfo1 = new com.faforever.commons.lobby.Player(2, "junit2", null, new Avatar("https://test.com/test.png", "junit"), "", new HashMap<>(), new HashMap<>());
    playerInfo2 = new com.faforever.commons.lobby.Player(3, "junit3", null, null, "", new HashMap<>(), new HashMap<>());

    Map<Integer, String> notes = new HashMap<>();
    notes.put(3, "junit3");
    Preferences preferences = PreferencesBuilder.create().defaultValues()
        .userPrefs()
        .setNotesByPlayerId(FXCollections.observableMap(notes))
        .then()
        .get();
    when(preferencesService.getPreferences()).thenReturn(preferences);

    when(userService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>());

    instance.afterPropertiesSet();
    instance.createOrUpdatePlayerForPlayerInfo(playerInfo1);
    instance.createOrUpdatePlayerForPlayerInfo(playerInfo2);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testPostConstruct() {
    verify(fafServerAccessor).addEventListener(eq(com.faforever.commons.lobby.PlayerInfo.class), any(Consumer.class));
    verify(fafServerAccessor).addEventListener(eq(SocialInfo.class), any(Consumer.class));
  }

  @Test
  public void testGetPlayerForUsernameUsernameDoesNotExist() {
    Optional<PlayerBean> player = instance.getPlayerByNameIfOnline("junit");
    assertFalse(player.isPresent());
  }

  @Test
  public void testGetPlayerForUsernameUsernameExists() {
    PlayerBean player = instance.getPlayerByNameIfOnline("junit2").orElseThrow();

    assertEquals("junit2", player.getUsername());
  }

  @Test
  public void testRegisterAndGetPlayerForUsernameNull() {
    assertThrows(IllegalArgumentException.class, () -> instance.createOrUpdatePlayerForPlayerInfo(null));
  }

  @Test
  public void testPlayerUpdatedFromPlayerInfo() {
    PlayerBean player = instance.getPlayerByNameIfOnline(playerInfo1.getLogin()).orElseThrow();

    assertEquals((int) playerInfo1.getRatings().values().stream().mapToInt(LeaderboardStats::getNumberOfGames).sum(), player.getNumberOfGames());
    assertEquals(playerInfo1.getClan(), player.getClan());
    assertEquals(playerInfo1.getCountry(), player.getCountry());

    instance.createOrUpdatePlayerForPlayerInfo(new com.faforever.commons.lobby.Player(2, "junit2", "ABC", null, "DE", new HashMap<>(), new HashMap<>()));

    assertEquals(0, player.getNumberOfGames());
    assertEquals("ABC", player.getClan());
    assertEquals("DE", player.getCountry());
  }

  @Test
  public void testGetPlayerNamesPopulated() {
    Set<String> playerNames = instance.getPlayerNames();
    assertThat(playerNames, hasSize(2));
  }

  @Test
  public void testGetPlayerNamesSomeInstances() {
    Set<String> playerNames = instance.getPlayerNames();

    assertThat(playerNames, hasSize(2));
    assertThat(playerNames, containsInAnyOrder(playerInfo1.getLogin(), playerInfo2.getLogin()));
  }

  @Test
  public void testAddFriend() {
    PlayerBean lisa = instance.getPlayerByNameIfOnline(playerInfo1.getLogin()).orElseThrow();
    PlayerBean ashley = instance.getPlayerByNameIfOnline(playerInfo2.getLogin()).orElseThrow();

    instance.addFriend(lisa);
    instance.addFriend(ashley);

    verify(fafServerAccessor).addFriend(lisa.getId());
    verify(fafServerAccessor).addFriend(ashley.getId());

    assertSame(lisa.getSocialStatus(), FRIEND);
    assertSame(ashley.getSocialStatus(), FRIEND);
  }

  @Test
  public void testAddFriendIsFoe() {
    PlayerBean player = instance.getPlayerByNameIfOnline(playerInfo1.getLogin()).orElseThrow();

    player.setSocialStatus(FOE);

    instance.addFriend(player);

    assertNotSame(player.getSocialStatus(), FOE);
  }

  @Test
  public void testRemoveFriend() {
    PlayerBean player1 = instance.getPlayerByNameIfOnline(playerInfo1.getLogin()).orElseThrow();
    PlayerBean player2 = instance.getPlayerByNameIfOnline(playerInfo2.getLogin()).orElseThrow();

    instance.addFriend(player1);
    verify(fafServerAccessor).addFriend(player1.getId());

    instance.addFriend(player2);
    verify(fafServerAccessor).addFriend(player2.getId());

    instance.removeFriend(player1);
    verify(fafServerAccessor).removeFriend(player1.getId());

    assertNotSame(player1.getSocialStatus(), FRIEND);
    assertSame(player2.getSocialStatus(), FRIEND);
  }

  @Test
  public void testAddFoe() {
    PlayerBean player1 = instance.getPlayerByNameIfOnline(playerInfo1.getLogin()).orElseThrow();
    PlayerBean player2 = instance.getPlayerByNameIfOnline(playerInfo2.getLogin()).orElseThrow();

    instance.addFoe(player1);
    instance.addFoe(player2);

    verify(fafServerAccessor).addFoe(player1.getId());
    verify(fafServerAccessor).addFoe(player2.getId());
    assertSame(player1.getSocialStatus(), FOE);
    assertSame(player2.getSocialStatus(), FOE);
  }

  @Test
  public void testAddFoeIsFriend() {
    PlayerBean player = instance.getPlayerByNameIfOnline(playerInfo1.getLogin()).orElseThrow();
    player.setSocialStatus(FRIEND);

    instance.addFoe(player);

    assertNotSame(player.getSocialStatus(), FRIEND);
  }

  @Test
  public void testRemoveFoe() {
    PlayerBean player = instance.getPlayerByNameIfOnline(playerInfo1.getLogin()).orElseThrow();

    instance.addFriend(player);
    instance.removeFriend(player);

    assertNotSame(player.getSocialStatus(), FRIEND);
  }

  @Test
  public void testGetCurrentPlayer() {
    PlayerBean currentPlayer = instance.getCurrentPlayer();

    assertThat(currentPlayer.getUsername(), is("junit"));
    assertThat(currentPlayer.getId(), is(1));
  }

  @Test
  public void testGetCurrentPlayerNull() {
    when(userService.getOwnPlayer()).thenReturn(null);

    assertThrows(IllegalStateException.class, () -> instance.getCurrentPlayer());
  }

  @Test
  public void testGetPlayerByName() {
    PlayerBean playerBean = PlayerBeanBuilder.create().defaultValues().get();
    Flux<ElideEntity> resultFlux = Flux.just(playerMapper.map(playerBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    Optional<PlayerBean> result = instance.getPlayerByName("junit").join();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().string("login").eq("junit"))));
    assertThat(result.orElse(null), is(playerBean));
  }

  @Test
  public void testGetPlayerByNamePlayerOnline() {
    instance.getPlayerByName("junit2").join();

    verify(fafApiAccessor, never()).getMany(any());
  }

  @Test
  public void testGetPlayersByIds() {
    PlayerBean playerBean = PlayerBeanBuilder.create().defaultValues().username("junit4").id(4).get();
    Flux<ElideEntity> resultFlux = Flux.just(playerMapper.map(playerBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    instance.getPlayersByIds(List.of(1, 2, 3, 4)).join();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("id").in(List.of(1, 4)))));
  }

  @Test
  public void testGetPlayersByIdsAllPlayersOnline() {
    instance.getPlayersByIds(List.of(2, 3)).join();

    verify(fafApiAccessor, never()).getMany(any());
  }

  @Test
  public void testEventBusRegistered() {
    verify(eventBus).register(instance);
  }

  @Test
  public void testPlayerLeftOpenGame() {
    PlayerBean player1 = playerMapper.update(playerInfo1, new PlayerBean());
    PlayerBean player2 = playerMapper.update(playerInfo2, new PlayerBean());
    Map<Integer, List<PlayerBean>> teams = new HashMap<>(Map.of(1, List.of(player1), 2, List.of(player2)));
    GameBean game = GameBeanBuilder.create().defaultValues().teams(teams).get();

    instance.updatePlayersInGame(game);

    assertThat(player1.getGame(), is(game));
    assertThat(player2.getGame(), is(game));

    teams.remove(1);

    instance.updatePlayersInGame(game);

    assertThat(player1.getGame(), is(nullValue()));
    assertThat(player2.getGame(), is(game));
  }

  @Test
  public void testThereIsFriendInGame() {
    ObservableMap<Integer, List<PlayerBean>> teams = FXCollections.observableMap(Map.of(1, List.of(playerMapper.update(playerInfo1, new PlayerBean()), playerMapper.update(playerInfo2, new PlayerBean()))));
    GameBean game = GameBeanBuilder.create().defaultValues().teams(teams).get();
    instance.createOrUpdatePlayerForPlayerInfo(playerInfo1);
    PlayerBean player1 = instance.getPlayerByNameIfOnline(playerInfo1.getLogin()).orElseThrow();
    instance.createOrUpdatePlayerForPlayerInfo(playerInfo2);
    instance.addFriend(player1);

    assertTrue(instance.areFriendsInGame(game));
  }

  @Test
  public void testNoFriendInGame() {
    ObservableMap<Integer, List<PlayerBean>> teams = FXCollections.observableMap(Map.of(1, List.of(playerMapper.update(playerInfo1, new PlayerBean()))));
    GameBean game = GameBeanBuilder.create().defaultValues().teams(teams).get();
    PlayerBean player2 = instance.getPlayerByNameIfOnline(playerInfo2.getLogin()).orElseThrow();
    player2.setId(100);
    instance.addFriend(player2);

    assertFalse(instance.areFriendsInGame(game));
    assertFalse(instance.areFriendsInGame(null));
  }

  @Test
  public void testCurrentPlayerInGame() {
    GameBean game = GameBeanBuilder.create().defaultValues().teams(Map.of(1, List.of(PlayerBeanBuilder.create().defaultValues().id(1).get()))).get();

    assertTrue(instance.isCurrentPlayerInGame(game));
  }

  @Test
  public void testCurrentPlayerNotInGame() {
    GameBean game = GameBeanBuilder.create().defaultValues().teams(Map.of(1, List.of(PlayerBeanBuilder.create().defaultValues().id(2).get()))).get();

    assertFalse(instance.isCurrentPlayerInGame(game));
  }

  @Test
  public void testGetCurrentAvatarByPlayerName() {
    when(avatarService.loadAvatar(any())).thenReturn(mock(Image.class));
    assertNotNull(instance.getCurrentAvatarByPlayerName("junit2").orElse(null));
  }

  @Test
  public void testPlayerOffline() {
    PlayerBean player = PlayerBeanBuilder.create().get();
    playerMapper.update(playerInfo1, player);

    instance.onPlayerOffline(new PlayerOfflineEvent(player));

    assertFalse(instance.getPlayerByNameIfOnline(playerInfo1.getLogin()).isPresent());
    assertFalse(instance.getPlayerByIdIfOnline(playerInfo1.getId()).isPresent());
  }

  @Test
  public void testAddPlayerNote() {
    PlayerBean player = PlayerBeanBuilder.create().id(2).get();
    assertFalse(instance.getNotesByPlayerId().containsKey(2));
    instance.updateNote(player, "junit");
    assertTrue(instance.getNotesByPlayerId().containsKey(2));
    verify(preferencesService).storeInBackground();
  }

  @Test
  public void testEditPlayerNote() {
    PlayerBean player = PlayerBeanBuilder.create().id(3).get();
    assertTrue(instance.getNotesByPlayerId().containsKey(3));
    instance.updateNote(player, "updated");
    assertEquals("updated", instance.getNotesByPlayerId().get(3));
    verify(preferencesService).storeInBackground();
  }

  @Test
  public void testRemovePlayerNote() {
    PlayerBean player = PlayerBeanBuilder.create().id(3).get();
    assertTrue(instance.getNotesByPlayerId().containsKey(3));
    instance.removeNote(player);
    assertFalse(instance.getNotesByPlayerId().containsKey(3));
    verify(preferencesService).storeInBackground();
  }

  @Test
  public void testNormalizeTextBeforeUpdatingPlayerNote() {
    PlayerBean player = PlayerBeanBuilder.create().id(2).get();
    instance.updateNote(player, "junit\n1\n\n2\n\n\n3\n4");
    assertEquals("junit\n1\n2\n3\n4", instance.getNotesByPlayerId().get(2));
  }
}
