package com.faforever.client.player;

import com.faforever.client.game.Game;
import com.faforever.client.game.GameAddedEvent;
import com.faforever.client.game.GameRemovedEvent;
import com.faforever.client.game.GameUpdatedEvent;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.PlayersMessage;
import com.faforever.client.remote.domain.SocialMessage;
import com.faforever.client.user.UserService;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.ReflectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static com.natpryce.hamcrest.reflection.HasAnnotationMatcher.hasAnnotation;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerServiceTest {

  @Mock
  private FafService fafService;
  @Mock
  private UserService userService;
  @Mock
  private EventBus eventBus;

  private PlayerService instance;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new PlayerService(fafService, userService, eventBus);

    when(fafService.connectionStateProperty()).thenReturn(new SimpleObjectProperty<>());

    instance.afterPropertiesSet();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testPostConstruct() throws Exception {
    verify(fafService).addOnMessageListener(eq(PlayersMessage.class), any(Consumer.class));
    verify(fafService).addOnMessageListener(eq(SocialMessage.class), any(Consumer.class));
  }

  @Test
  public void testGetPlayerForUsernameUsernameDoesNotExist() throws Exception {
    Optional<Player> player = instance.getPlayerForUsername("junit");
    assertFalse(player.isPresent());
  }

  @Test
  public void testGetPlayerForUsernameUsernameExists() throws Exception {
    instance.createAndGetPlayerForUsername("junit");

    Optional<Player> player = instance.getPlayerForUsername("junit");

    assertTrue(player.isPresent());
    assertEquals("junit", player.get().getUsername());
  }

  @Test
  public void testRegisterAndGetPlayerForUsernameDoesNotExist() throws Exception {
    Player player = instance.createAndGetPlayerForUsername("junit");

    assertNotNull(player);
    assertEquals("junit", player.getUsername());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRegisterAndGetPlayerForUsernameNull() throws Exception {
    instance.createAndGetPlayerForUsername(null);
  }

  @Test
  public void testGetPlayerNamesEmpty() throws Exception {
    Set<String> playerNames = instance.getPlayerNames();
    assertThat(playerNames, empty());
  }

  @Test
  public void testGetPlayerNamesSomeInstances() throws Exception {
    instance.createAndGetPlayerForUsername("player1");
    instance.createAndGetPlayerForUsername("player2");

    Set<String> playerNames = instance.getPlayerNames();

    assertThat(playerNames, hasSize(2));
    assertThat(playerNames, containsInAnyOrder("player1", "player2"));
  }

  @Test
  public void testAddFriend() throws Exception {
    Player lisa = instance.createAndGetPlayerForUsername("lisa");
    Player ashley = instance.createAndGetPlayerForUsername("ashley");

    instance.addFriend(lisa);
    instance.addFriend(ashley);

    verify(fafService).addFriend(lisa);
    verify(fafService).addFriend(ashley);

    assertTrue("Property 'friend' was not set to true", lisa.getSocialStatus() == FRIEND);
    assertTrue("Property 'friend' was not set to true", ashley.getSocialStatus() == FRIEND);
  }

  @Test
  public void testAddFriendIsFoe() throws Exception {
    Player player = instance.createAndGetPlayerForUsername("player");
    player.setSocialStatus(FOE);

    instance.addFriend(player);

    assertFalse("Property 'foe' is still true", player.getSocialStatus() == FOE);
  }

  @Test
  public void testRemoveFriend() throws Exception {
    Player player1 = instance.createAndGetPlayerForUsername("player1");
    Player player2 = instance.createAndGetPlayerForUsername("player2");

    instance.addFriend(player1);
    verify(fafService).addFriend(player1);

    instance.addFriend(player2);
    verify(fafService).addFriend(player2);

    instance.removeFriend(player1);
    verify(fafService).removeFriend(player1);

    assertFalse("Property 'friend' was not set to false", player1.getSocialStatus() == FRIEND);
    assertTrue("Property 'friend' was not set to true", player2.getSocialStatus() == FRIEND);
  }

  @Test
  public void testAddFoe() throws Exception {
    Player player1 = instance.createAndGetPlayerForUsername("player1");
    Player player2 = instance.createAndGetPlayerForUsername("player2");

    instance.addFoe(player1);
    instance.addFoe(player2);

    verify(fafService).addFoe(player1);
    verify(fafService).addFoe(player2);
    assertTrue("Property 'foe' was not set to true", player1.getSocialStatus() == FOE);
    assertTrue("Property 'foe' was not set to true", player2.getSocialStatus() == FOE);
  }

  @Test
  public void testAddFoeIsFriend() throws Exception {
    Player player = instance.createAndGetPlayerForUsername("player");
    player.setSocialStatus(FRIEND);

    instance.addFoe(player);

    assertFalse("Property 'friend' is still true", player.getSocialStatus() == FRIEND);
  }

  @Test
  public void testRemoveFoe() throws Exception {
    Player player = instance.createAndGetPlayerForUsername("player");

    instance.addFriend(player);
    instance.removeFriend(player);

    assertFalse("Property 'friend' was not set to false", player.getSocialStatus() == FRIEND);
  }

  @Test
  public void testGetCurrentPlayer() throws Exception {
    LoginSuccessEvent event = new LoginSuccessEvent("junit", "", 1);
    instance.onLoginSuccess(event);

    Player currentPlayer = instance.getCurrentPlayer().orElseThrow(() -> new IllegalStateException("No player returned"));

    assertThat(currentPlayer.getUsername(), is("junit"));
    assertThat(currentPlayer.getId(), is(1));
  }

  @Test
  public void testSubscribeAnnotations() {
    assertThat(ReflectionUtils.findMethod(instance.getClass(), "onLoginSuccess", LoginSuccessEvent.class),
        hasAnnotation(Subscribe.class));
  }

  @Test
  public void testEventBusRegistered() throws Exception {
    verify(eventBus).register(instance);
  }

  @Test
  public void onGameRemoved() {
    Game game = new Game();
    ObservableMap<String, List<String>> teams = game.getTeams();
    teams.put("1", Collections.singletonList("JUnit1"));
    teams.put("2", Collections.singletonList("JUnit2"));

    Player player1 = instance.createAndGetPlayerForUsername("JUnit1");
    Player player2 = instance.createAndGetPlayerForUsername("JUnit2");

    instance.onGameAdded(new GameAddedEvent(game));

    assertThat(player1.getGame(), is(game));
    assertThat(player2.getGame(), is(game));

    instance.onGameRemoved(new GameRemovedEvent(game));

    assertThat(player1.getGame(), is(nullValue()));
    assertThat(player2.getGame(), is(nullValue()));
  }

  @Test
  public void testPlayerLeftOpenGame() {
    Game game = new Game();
    game.setStatus(GameStatus.OPEN);
    ObservableMap<String, List<String>> teams = game.getTeams();
    teams.put("1", Collections.singletonList("JUnit1"));
    teams.put("2", Collections.singletonList("JUnit2"));

    Player player1 = instance.createAndGetPlayerForUsername("JUnit1");
    Player player2 = instance.createAndGetPlayerForUsername("JUnit2");
    game.setHost("JUnit2");

    instance.onGameAdded(new GameAddedEvent(game));

    assertThat(player1.getGame(), is(game));
    assertThat(player2.getGame(), is(game));

    teams.remove("1");

    instance.onGameUpdated(new GameUpdatedEvent(game));

    assertThat(player1.getGame(), is(nullValue()));
    assertThat(player2.getGame(), is(game));
  }

  @Test
  public void testGameRemovedFromPlayerIfGameClosed() {
    Game game = new Game();
    game.setStatus(GameStatus.OPEN);
    ObservableMap<String, List<String>> teams = game.getTeams();
    teams.put("1", Collections.singletonList("JUnit1"));
    teams.put("2", Collections.singletonList("JUnit2"));

    Player player1 = instance.createAndGetPlayerForUsername("JUnit1");
    Player player2 = instance.createAndGetPlayerForUsername("JUnit2");
    game.setHost("JUnit2");

    instance.onGameAdded(new GameAddedEvent(game));

    assertThat(player1.getGame(), is(game));
    assertThat(player2.getGame(), is(game));

    game.setStatus(GameStatus.CLOSED);

    instance.onGameUpdated(new GameUpdatedEvent(game));

    assertThat(player1.getGame(), is(nullValue()));
    assertThat(player2.getGame(), is(nullValue()));
  }

  @Test
  public void testFriendsOnline() {
    final Player ta4Life = instance.createAndGetPlayerForUsername("TA4Life");
    ta4Life.setId(16);
    final Player gyle = instance.createAndGetPlayerForUsername("Gyle");
    gyle.setId(333);
    final Player zock = instance.createAndGetPlayerForUsername("Zock");
    ta4Life.setId(6516);

    instance.addFriend(ta4Life);
    instance.addFriend(gyle);
    instance.addFoe(zock);

    assertThat(instance.friendsOnline().size(), is(2));
    assertThat(instance.friendsOnline().contains(ta4Life), is(true));
    assertThat(instance.friendsOnline().contains(gyle), is(true));
    assertThat(instance.friendsOnline().contains(zock), is(false));
  }

  @Test
  public void testNoFriendsOnline() {
    assertThat(instance.friendsOnline().size(), is(0));
  }

  @Test
  public void testThereAreNoFriendsInGame() {
    Game game = new Game();
    game.setTitle("Test game");
    ObservableMap<String, List<String>> teams = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());
    teams.putIfAbsent("1", List.of("Gyle", "TA4Life"));
    teams.putIfAbsent("2", List.of("ZePilot", "Downlord", "ZLO"));
    game.setTeams(teams);

    assertThat(instance.friendsInGame(game), is(Collections.emptyList()));
  }

  @Test
  public void testAreMyFriendsInGame() {
    final Player ta4Life = instance.createAndGetPlayerForUsername("TA4Life");
    ta4Life.setId(16);
    final Player gyle = instance.createAndGetPlayerForUsername("Gyle");
    gyle.setId(333);
    instance.addFriend(ta4Life);
    instance.addFriend(gyle);

    Game game = new Game();
    game.setTitle("Test game");
    ObservableMap<String, List<String>> teams = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());
    teams.putIfAbsent("1", List.of(gyle.getUsername(), "TA4Life"));
    teams.putIfAbsent("2", List.of("ZePilot", "Downlord", "ZLO"));
    game.setTeams(teams);

    assertThat(instance.friendsInGame(game), is(List.of(ta4Life, gyle)));
  }
}
