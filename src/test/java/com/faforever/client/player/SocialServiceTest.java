package com.faforever.client.player;

import com.faforever.client.builders.GameInfoBuilder;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.preferences.UserPrefs;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.social.SocialService;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.lobby.SocialInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.scheduler.Schedulers;
import reactor.test.publisher.TestPublisher;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SocialServiceTest extends ServiceTest {

  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private PlayerService playerService;
  @Mock
  private FxApplicationThreadExecutor fxApplicationThreadExecutor;
  @Spy
  private UserPrefs userPrefs;

  @InjectMocks
  private SocialService instance;

  private PlayerInfo player1;
  private PlayerInfo player2;

  private final TestPublisher<SocialInfo> socialInfoTestPublisher = TestPublisher.create();

  @BeforeEach
  public void setUp() throws Exception {
    when(fxApplicationThreadExecutor.asScheduler()).thenReturn(Schedulers.immediate());
    when(fafServerAccessor.getEvents(SocialInfo.class)).thenReturn(socialInfoTestPublisher.flux());
    PlayerInfo currentPlayer = PlayerInfoBuilder.create().defaultValues().id(1).username("junit").get();
    player1 = PlayerInfoBuilder.create().defaultValues().id(2).username("junit2").get();
    player2 = PlayerInfoBuilder.create().defaultValues().id(3).username("junit3").get();

    lenient().when(playerService.getCurrentPlayer()).thenReturn(currentPlayer);
    lenient().when(playerService.getPlayerByIdIfOnline(2)).thenReturn(Optional.of(player1));
    lenient().when(playerService.getPlayerByIdIfOnline(3)).thenReturn(Optional.of(player2));

    userPrefs.getNotesByPlayerId().put(3, "junit3");

    instance.afterPropertiesSet();

    socialInfoTestPublisher.assertSubscribers(1);
  }

  @Test
  public void testSocialInfo() {
    socialInfoTestPublisher.next(new SocialInfo(List.of(), List.of(), List.of(2), List.of(3), 0));

    assertEquals(FRIEND, player1.getSocialStatus());
    assertEquals(FOE, player2.getSocialStatus());
  }

  @Test
  public void testAddFriend() {
    instance.addFriend(player1);
    instance.addFriend(player2);

    verify(fafServerAccessor).addFriend(player1.getId());
    verify(fafServerAccessor).addFriend(player2.getId());

    assertEquals(player1.getSocialStatus(), FRIEND);
    assertEquals(player2.getSocialStatus(), FRIEND);
  }

  @Test
  public void testAddFriendIsFoe() {
    player1.setSocialStatus(FOE);

    instance.addFriend(player1);

    assertNotEquals(player1.getSocialStatus(), FOE);
  }

  @Test
  public void testRemoveFriend() {
    instance.addFriend(player1);
    verify(fafServerAccessor).addFriend(player1.getId());

    instance.addFriend(player2);
    verify(fafServerAccessor).addFriend(player2.getId());

    instance.removeFriend(player1);
    verify(fafServerAccessor).removeFriend(player1.getId());

    assertNotEquals(player1.getSocialStatus(), FRIEND);
    assertEquals(player2.getSocialStatus(), FRIEND);
  }

  @Test
  public void testAddFoe() {
    instance.addFoe(player1);
    instance.addFoe(player2);

    verify(fafServerAccessor).addFoe(player1.getId());
    verify(fafServerAccessor).addFoe(player2.getId());
    assertEquals(player1.getSocialStatus(), FOE);
    assertEquals(player2.getSocialStatus(), FOE);
  }

  @Test
  public void testAddFoeIsFriend() {
    player1.setSocialStatus(FRIEND);

    instance.addFoe(player1);

    assertNotEquals(player1.getSocialStatus(), FRIEND);
  }

  @Test
  public void testRemoveFoe() {
    instance.addFriend(player1);
    instance.removeFriend(player1);

    assertNotEquals(player1.getSocialStatus(), FRIEND);
  }

  @Test
  public void testThereIsFriendInGame() {
    Map<Integer, List<Integer>> teams = Map.of(1, List.of(1, 2));
    GameInfo game = GameInfoBuilder.create().defaultValues().teams(teams).get();
    instance.addFriend(player1);

    assertTrue(instance.areFriendsInGame(game));
  }

  @Test
  public void testNoFriendInGame() {
    Map<Integer, List<Integer>> teams = Map.of(1, List.of(1));
    GameInfo game = GameInfoBuilder.create().defaultValues().teams(teams).get();
    player2.setId(100);
    instance.addFriend(player2);

    assertFalse(instance.areFriendsInGame(game));
    assertFalse(instance.areFriendsInGame(null));
  }

  @Test
  public void testAddPlayerNote() {
    PlayerInfo player = PlayerInfoBuilder.create().id(2).get();
    assertFalse(userPrefs.getNotesByPlayerId().containsKey(2));
    instance.updateNote(player, "junit");
    assertTrue(userPrefs.getNotesByPlayerId().containsKey(2));
  }

  @Test
  public void testEditPlayerNote() {
    PlayerInfo player = PlayerInfoBuilder.create().id(3).get();
    assertTrue(userPrefs.getNotesByPlayerId().containsKey(3));
    instance.updateNote(player, "updated");
    assertEquals("updated", userPrefs.getNotesByPlayerId().get(3));
  }

  @Test
  public void testRemovePlayerNote() {
    PlayerInfo player = PlayerInfoBuilder.create().id(3).get();
    assertTrue(userPrefs.getNotesByPlayerId().containsKey(3));
    instance.removeNote(player);
    assertFalse(userPrefs.getNotesByPlayerId().containsKey(3));
  }

  @Test
  public void testNormalizeTextBeforeUpdatingPlayerNote() {
    PlayerInfo player = PlayerInfoBuilder.create().id(2).get();
    instance.updateNote(player, "junit\n1\n\n2\n\n\n3\n4");
    assertEquals("junit\n1\n2\n3\n4", userPrefs.getNotesByPlayerId().get(2));
  }
}
