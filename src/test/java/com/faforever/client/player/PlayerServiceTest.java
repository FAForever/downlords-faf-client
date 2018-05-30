package com.faforever.client.player;

import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.PlayersMessage;
import com.faforever.client.remote.domain.SocialMessage;
import com.faforever.client.user.UserService;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.ReflectionUtils;

import java.util.Set;
import java.util.function.Consumer;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static com.natpryce.hamcrest.reflection.HasAnnotationMatcher.hasAnnotation;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

    instance.postConstruct();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testPostConstruct() throws Exception {
    verify(fafService).addOnMessageListener(eq(PlayersMessage.class), any(Consumer.class));
    verify(fafService).addOnMessageListener(eq(SocialMessage.class), any(Consumer.class));
  }

  @Test
  public void testGetPlayerForUsernameUsernameDoesNotExist() throws Exception {
    Player player = instance.getPlayerForUsername("junit");
    assertNull(player);
  }

  @Test
  public void testGetPlayerForUsernameUsernameExists() throws Exception {
    instance.createAndGetPlayerForUsername("junit");

    Player player = instance.getPlayerForUsername("junit");

    assertNotNull(player);
    assertEquals("junit", player.getUsername());
  }

  @Test
  public void testGetPlayerForUsernameNull() throws Exception {
    Player player = instance.getPlayerForUsername(null);
    assertNull(player);
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
}
