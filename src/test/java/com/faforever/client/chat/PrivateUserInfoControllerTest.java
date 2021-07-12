package com.faforever.client.chat;

import com.faforever.client.achievements.AchievementService;
import com.faforever.client.game.GameBuilder;
import com.faforever.client.game.GameDetailController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.Leaderboard;
import com.faforever.client.leaderboard.LeaderboardBuilder;
import com.faforever.client.leaderboard.LeaderboardRatingBuilder;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.test.UITest;
import com.faforever.client.vault.replay.WatchButtonController;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PrivateUserInfoControllerTest extends UITest {

  @Mock
  private I18n i18n;
  @Mock
  private AchievementService achievementService;
  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private EventBus eventBus;
  @Mock
  private ChatUserService chatUserService;
  @Mock
  private GameDetailController gameDetailController;
  @Mock
  private WatchButtonController watchButtonController;


  private PrivateUserInfoController instance;
  private Player player;
  private ChatChannelUser chatChannelUser;
  private Leaderboard leaderboard;

  @BeforeEach
  public void setUp() throws Exception {
    leaderboard = LeaderboardBuilder.create().defaultValues().technicalName("global").get();
    player = PlayerBuilder.create("junit").defaultValues().game(null).get();
    chatChannelUser = ChatChannelUserBuilder.create("junit").defaultValues().displayed(false).player(player).get();

    when(achievementService.getPlayerAchievements(player.getId())).thenReturn(CompletableFuture.completedFuture(List.of()));
    when(achievementService.getAchievementDefinitions()).thenReturn(CompletableFuture.completedFuture(List.of()));
    when(leaderboardService.getLeaderboards()).thenReturn(CompletableFuture.completedFuture(List.of(leaderboard)));
    when(i18n.getOrDefault(leaderboard.getTechnicalName(), leaderboard.getNameKey())).thenReturn(leaderboard.getTechnicalName());
    when(i18n.get("leaderboard.rating", leaderboard.getTechnicalName())).thenReturn(leaderboard.getTechnicalName());
    when(i18n.get(eq("chat.privateMessage.achievements.unlockedFormat"), any(), any())).thenReturn("0/0");
    when(i18n.number(anyInt())).thenReturn("123");

    instance = new PrivateUserInfoController(i18n, achievementService,
        leaderboardService, eventBus, chatUserService);

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
    instance.setChatUser(chatChannelUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(chatChannelUser.isDisplayed());
    assertTrue(instance.userImageView.isVisible());
    assertTrue(instance.countryLabel.isVisible());
    assertTrue(instance.ratingsLabels.isVisible());
    assertTrue(instance.ratingsValues.isVisible());
    assertTrue(instance.gamesPlayedLabel.isVisible());
    assertTrue(instance.gamesPlayedLabelLabel.isVisible());
    assertTrue(instance.unlockedAchievementsLabel.isVisible());
    assertTrue(instance.unlockedAchievementsLabelLabel.isVisible());
    assertNotNull(instance.userImageView.getImage());
    assertFalse(instance.gameDetailWrapper.isVisible());
    assertTrue(instance.ratingsLabels.getText().contains(leaderboard.getTechnicalName()));
    assertTrue(instance.ratingsValues.getText().contains("123"));
    assertEquals("0/0", instance.unlockedAchievementsLabel.getText());
    assertEquals("123", instance.gamesPlayedLabel.getText());
    verify(chatUserService).associatePlayerToChatUser(chatChannelUser, player);
    verify(gameDetailController, times(2)).setGame(player.getGame());
    verify(achievementService).getPlayerAchievements(player.getId());
  }

  @Test
  public void testSetChatUserWithPlayerWithGame() {
    player.setGame(GameBuilder.create().defaultValues().get());
    instance.setChatUser(chatChannelUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.gameDetailWrapper.isVisible());
    verify(gameDetailController).setGame(player.getGame());
  }

  @Test
  public void testSetChatUserWithPlayerNoGameThenJoinsGame() {
    instance.setChatUser(chatChannelUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.gameDetailWrapper.isVisible());
    verify(gameDetailController, times(2)).setGame(player.getGame());

    player.setGame(GameBuilder.create().defaultValues().get());

    assertTrue(instance.gameDetailWrapper.isVisible());
    verify(gameDetailController, times(1)).setGame(player.getGame());
  }

  @Test
  public void testSetChatUserLeavesGame() {
    instance.setChatUser(chatChannelUser);
    player.setGame(GameBuilder.create().defaultValues().get());
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.gameDetailWrapper.isVisible());
    verify(gameDetailController, times(1)).setGame(player.getGame());

    player.setGame(null);

    assertFalse(instance.gameDetailWrapper.isVisible());
    verify(gameDetailController, times(3)).setGame(player.getGame());
  }

  @Test
  public void testRatingChange() {
    instance.setChatUser(chatChannelUser);
    WaitForAsyncUtils.waitForFxEvents();

    player.getLeaderboardRatings().put("test", LeaderboardRatingBuilder.create().defaultValues().get());
    WaitForAsyncUtils.waitForFxEvents();

    verify(leaderboardService, times(2)).getLeaderboards();
  }

  @Test
  public void testSetChatUserWithNoPlayer() {
    chatChannelUser.setPlayer(null);
    instance.setChatUser(chatChannelUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.userImageView.isVisible());
    assertFalse(instance.countryLabel.isVisible());
    assertFalse(instance.ratingsLabels.isVisible());
    assertFalse(instance.ratingsValues.isVisible());
    assertFalse(instance.gamesPlayedLabel.isVisible());
    assertFalse(instance.gamesPlayedLabelLabel.isVisible());
    assertFalse(instance.unlockedAchievementsLabel.isVisible());
    assertFalse(instance.unlockedAchievementsLabelLabel.isVisible());
    verify(gameDetailController, times(2)).setGame(null);
  }

  @Test
  public void testSetChatUserWithNoPlayerThenGetsPlayer() {
    chatChannelUser.setPlayer(null);
    instance.setChatUser(chatChannelUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.userImageView.isVisible());
    assertFalse(instance.countryLabel.isVisible());
    assertFalse(instance.ratingsLabels.isVisible());
    assertFalse(instance.ratingsValues.isVisible());
    assertFalse(instance.gamesPlayedLabel.isVisible());
    assertFalse(instance.gamesPlayedLabelLabel.isVisible());
    assertFalse(instance.unlockedAchievementsLabel.isVisible());
    assertFalse(instance.unlockedAchievementsLabelLabel.isVisible());
    verify(gameDetailController, times(2)).setGame(null);

    chatChannelUser.setPlayer(player);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(chatChannelUser.isDisplayed());
    assertTrue(instance.userImageView.isVisible());
    assertTrue(instance.countryLabel.isVisible());
    assertTrue(instance.ratingsLabels.isVisible());
    assertTrue(instance.ratingsValues.isVisible());
    assertTrue(instance.gamesPlayedLabel.isVisible());
    assertTrue(instance.gamesPlayedLabelLabel.isVisible());
    assertTrue(instance.unlockedAchievementsLabel.isVisible());
    assertTrue(instance.unlockedAchievementsLabelLabel.isVisible());
    assertNotNull(instance.userImageView.getImage());
    assertFalse(instance.gameDetailWrapper.isVisible());
    assertTrue(instance.ratingsLabels.getText().contains(leaderboard.getTechnicalName()));
    assertTrue(instance.ratingsValues.getText().contains("123"));
    assertEquals("0/0", instance.unlockedAchievementsLabel.getText());
    assertEquals("123", instance.gamesPlayedLabel.getText());
    verify(chatUserService).associatePlayerToChatUser(chatChannelUser, player);
    verify(gameDetailController, times(3)).setGame(player.getGame());
    verify(achievementService).getPlayerAchievements(player.getId());
  }

  @Test
  public void testGetRoot() {
    instance.setChatUser(chatChannelUser);
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(instance.privateUserInfoRoot, instance.getRoot());
  }
}
