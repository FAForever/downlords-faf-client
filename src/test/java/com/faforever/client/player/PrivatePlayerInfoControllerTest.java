package com.faforever.client.player;

import com.faforever.client.achievements.AchievementService;
import com.faforever.client.builders.ChatChannelUserBuilder;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.LeaderboardBeanBuilder;
import com.faforever.client.builders.LeaderboardRatingBeanBuilder;
import com.faforever.client.builders.LeaderboardRatingMapBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.GameDetailController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.replay.WatchButtonController;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PrivatePlayerInfoControllerTest extends PlatformTest {
  private static final String CHANNEL_NAME = "testChannel";
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
  private PlayerBean player;
  private ChatChannelUser chatChannelUser;
  private LeaderboardBean leaderboard;

  @BeforeEach
  public void setUp() throws Exception {
    leaderboard = LeaderboardBeanBuilder.create().defaultValues().technicalName("global").get();
    player = PlayerBeanBuilder.create().defaultValues().game(null).get();
    chatChannelUser = ChatChannelUserBuilder.create(USERNAME, CHANNEL_NAME).defaultValues().player(player).get();

    when(gameDetailController.gameProperty()).thenReturn(new SimpleObjectProperty<>());
    when(achievementService.getPlayerAchievements(player.getId())).thenReturn(CompletableFuture.completedFuture(List.of()));
    when(achievementService.getAchievementDefinitions()).thenReturn(CompletableFuture.completedFuture(List.of()));
    when(leaderboardService.getLeaderboards()).thenReturn(CompletableFuture.completedFuture(List.of(leaderboard)));
    when(i18n.getOrDefault(leaderboard.getTechnicalName(), leaderboard.getNameKey())).thenReturn(leaderboard.getTechnicalName());
    when(i18n.get("leaderboard.rating", leaderboard.getTechnicalName())).thenReturn(leaderboard.getTechnicalName());
    when(i18n.get(eq("chat.privateMessage.achievements.unlockedFormat"), any(), any())).thenReturn("0/0");
    when(i18n.number(anyInt())).thenReturn("123");

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
    player.setLeaderboardRatings(LeaderboardRatingMapBuilder.create().put(leaderboard.getTechnicalName(), LeaderboardRatingBeanBuilder.create().defaultValues().get()).get());

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
    assertTrue(instance.ratingsLabels.getText().contains(leaderboard.getTechnicalName()));
    assertTrue(instance.ratingsValues.getText().contains("123"));
    assertEquals("0/0", instance.unlockedAchievements.getText());
    assertEquals("123", instance.gamesPlayed.getText());
    verify(achievementService).getPlayerAchievements(player.getId());
  }

  @Test
  public void testSetChatUserWithPlayerWithGame() {
    player.setGame(GameBeanBuilder.create().defaultValues().get());
    instance.setChatUser(chatChannelUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.gameDetailWrapper.isVisible());
  }

  @Test
  public void testSetChatUserWithPlayerNoGameThenJoinsGame() {
    instance.setChatUser(chatChannelUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.gameDetailWrapper.isVisible());

    player.setGame(GameBeanBuilder.create().defaultValues().get());

    assertTrue(instance.gameDetailWrapper.isVisible());
  }

  @Test
  public void testSetChatUserLeavesGame() {
    instance.setChatUser(chatChannelUser);
    player.setGame(GameBeanBuilder.create().defaultValues().get());
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.gameDetailWrapper.isVisible());

    player.setGame(null);

    assertFalse(instance.gameDetailWrapper.isVisible());
  }

  @Test
  public void testRatingChange() {
    instance.setChatUser(chatChannelUser);
    WaitForAsyncUtils.waitForFxEvents();

    player.setLeaderboardRatings(Map.of("test", LeaderboardRatingBeanBuilder.create().defaultValues().get()));
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

    player.setLeaderboardRatings(LeaderboardRatingMapBuilder.create().put(leaderboard.getTechnicalName(), LeaderboardRatingBeanBuilder.create().defaultValues().get()).get());
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
    assertTrue(instance.ratingsLabels.getText().contains(leaderboard.getTechnicalName()));
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
