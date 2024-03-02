package com.faforever.client.player;

import com.faforever.client.achievements.AchievementService;
import com.faforever.client.builders.ChatChannelUserBuilder;
import com.faforever.client.builders.GameInfoBuilder;
import com.faforever.client.builders.LeaderboardRatingMapBuilder;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.chat.ChatChannel;
import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.domain.api.Leaderboard;
import com.faforever.client.domain.api.LeaderboardRating;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.game.GameDetailController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.replay.WatchButtonController;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.beans.property.SimpleObjectProperty;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

public class PrivatePlayerInfoControllerTest extends PlatformTest {
  private static final String USERNAME = "junit";

  @Mock
  private UiService uiService;
  @Mock
  private I18n i18n;
  @Mock
  private AchievementService achievementService;
  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private GameDetailController gameDetailController;
  @Mock
  private WatchButtonController watchButtonController;


  @InjectMocks
  private PrivatePlayerInfoController instance;
  private PlayerInfo player;
  private ChatChannelUser chatChannelUser;
  private Leaderboard leaderboard;

  @BeforeEach
  public void setUp() throws Exception {
    leaderboard = Instancio.of(Leaderboard.class).set(field(Leaderboard::technicalName), "global").create();
    player = PlayerInfoBuilder.create().defaultValues().game(null).get();
    chatChannelUser = ChatChannelUserBuilder.create(USERNAME, new ChatChannel("testChannel"))
                                            .defaultValues()
                                            .player(player)
                                            .get();

    lenient().when(gameDetailController.gameProperty()).thenReturn(new SimpleObjectProperty<>());
    lenient().when(achievementService.getPlayerAchievements(player.getId())).thenReturn(Flux.empty());
    lenient().when(achievementService.getAchievementDefinitions()).thenReturn(Flux.empty());
    lenient().when(leaderboardService.getLeaderboards()).thenReturn(Flux.just(leaderboard));
    lenient().when(i18n.getOrDefault(leaderboard.technicalName(), leaderboard.nameKey()))
             .thenReturn(leaderboard.technicalName());
    lenient().when(i18n.get("leaderboard.rating", leaderboard.technicalName())).thenReturn(leaderboard.technicalName());
    lenient().when(i18n.get(eq("chat.privateMessage.achievements.unlockedFormat"), any(), any())).thenReturn("0/0");
    lenient().when(i18n.number(anyInt())).thenReturn("123");

    loadFxml("theme/chat/private_user_info.fxml", clazz -> {
      if (clazz == GameDetailController.class) {
        return gameDetailController;
      }
      if (clazz == WatchButtonController.class) {
        return watchButtonController;
      }
      return instance;
    });
  }

  @Test
  public void testSetChatUserWithPlayerNoGame() {
    player.setLeaderboardRatings(LeaderboardRatingMapBuilder.create()
                                                            .put(leaderboard.technicalName(),
                                                                 Instancio.create(LeaderboardRating.class))
                                                            .get());

    instance.setChatUser(chatChannelUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.userImageView.isVisible());
    assertTrue(instance.country.isVisible());
    assertTrue(instance.ratingsLabels.isVisible());
    assertTrue(instance.ratingsValues.isVisible());
    assertTrue(instance.gamesPlayed.isVisible());
    assertTrue(instance.gamesPlayedLabel.isVisible());
    assertTrue(instance.unlockedAchievements.isVisible());
    assertTrue(instance.unlockedAchievementsLabel.isVisible());
    assertNotNull(instance.userImageView.getImage());
    assertFalse(instance.gameDetailWrapper.isVisible());
    assertTrue(instance.ratingsLabels.getText().contains(leaderboard.technicalName()));
    assertTrue(instance.ratingsValues.getText().contains("123"));
    assertEquals("0/0", instance.unlockedAchievements.getText());
    assertEquals("123", instance.gamesPlayed.getText());
    verify(achievementService).getPlayerAchievements(player.getId());
  }

  @Test
  public void testSetChatUserWithPlayerWithGame() {
    player.setGame(GameInfoBuilder.create().defaultValues().get());
    instance.setChatUser(chatChannelUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.gameDetailWrapper.isVisible());
  }

  @Test
  public void testSetChatUserWithPlayerNoGameThenJoinsGame() {
    instance.setChatUser(chatChannelUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.gameDetailWrapper.isVisible());

    player.setGame(GameInfoBuilder.create().defaultValues().get());

    assertTrue(instance.gameDetailWrapper.isVisible());
  }

  @Test
  public void testSetChatUserLeavesGame() {
    instance.setChatUser(chatChannelUser);
    player.setGame(GameInfoBuilder.create().defaultValues().get());
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.gameDetailWrapper.isVisible());

    player.setGame(null);

    assertFalse(instance.gameDetailWrapper.isVisible());
  }

  @Test
  public void testRatingChange() {
    instance.setChatUser(chatChannelUser);
    WaitForAsyncUtils.waitForFxEvents();

    player.setLeaderboardRatings(Map.of("test", Instancio.create(LeaderboardRating.class)));
    WaitForAsyncUtils.waitForFxEvents();

    verify(leaderboardService).getLeaderboards();
  }

  @Test
  public void testSetChatUserWithNoPlayer() {
    chatChannelUser.setPlayer(null);
    instance.setChatUser(chatChannelUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.userImageView.isVisible());
    assertFalse(instance.country.isVisible());
    assertFalse(instance.ratingsLabels.isVisible());
    assertFalse(instance.ratingsValues.isVisible());
    assertFalse(instance.gamesPlayed.isVisible());
    assertFalse(instance.gamesPlayedLabel.isVisible());
    assertFalse(instance.unlockedAchievements.isVisible());
    assertFalse(instance.unlockedAchievementsLabel.isVisible());
  }

  @Test
  public void testSetChatUserWithNoPlayerThenGetsPlayer() {
    chatChannelUser.setPlayer(null);
    instance.setChatUser(chatChannelUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.userImageView.isVisible());
    assertFalse(instance.country.isVisible());
    assertFalse(instance.ratingsLabels.isVisible());
    assertFalse(instance.ratingsValues.isVisible());
    assertFalse(instance.gamesPlayed.isVisible());
    assertFalse(instance.gamesPlayedLabel.isVisible());
    assertFalse(instance.unlockedAchievements.isVisible());
    assertFalse(instance.unlockedAchievementsLabel.isVisible());

    player.setLeaderboardRatings(LeaderboardRatingMapBuilder.create()
                                                            .put(leaderboard.technicalName(),
                                                                 Instancio.create(LeaderboardRating.class))
                                                            .get());
    chatChannelUser.setPlayer(player);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.userImageView.isVisible());
    assertTrue(instance.country.isVisible());
    assertTrue(instance.ratingsLabels.isVisible());
    assertTrue(instance.ratingsValues.isVisible());
    assertTrue(instance.gamesPlayed.isVisible());
    assertTrue(instance.gamesPlayedLabel.isVisible());
    assertTrue(instance.unlockedAchievements.isVisible());
    assertTrue(instance.unlockedAchievementsLabel.isVisible());
    assertNotNull(instance.userImageView.getImage());
    assertFalse(instance.gameDetailWrapper.isVisible());
    assertTrue(instance.ratingsLabels.getText().contains(leaderboard.technicalName()));
    assertTrue(instance.ratingsValues.getText().contains("123"));
    assertEquals("0/0", instance.unlockedAchievements.getText());
    assertEquals("123", instance.gamesPlayed.getText());
    verify(achievementService).getPlayerAchievements(player.getId());
  }

  @Test
  public void testGetRoot() {
    instance.setChatUser(chatChannelUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(instance.privateUserInfoRoot, instance.getRoot());
  }
}
