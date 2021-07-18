package com.faforever.client.achievements;

import com.faforever.client.achievements.AchievementService.AchievementState;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.remote.FafService;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.dto.AchievementDefinition;
import com.faforever.commons.api.dto.PlayerAchievement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.faforever.client.achievements.AchievementService.AchievementState.HIDDEN;
import static com.faforever.client.achievements.AchievementService.AchievementState.REVEALED;
import static com.faforever.client.achievements.AchievementService.AchievementState.UNLOCKED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AchievementServiceTest extends ServiceTest {

  private static final int PLAYER_ID = 123;

  @Mock
  private PlayerService playerService;
  @Mock
  private AchievementService instance;
  @Mock
  private FafService fafService;
  @Mock
  private AssetService assetService;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new AchievementService(fafService, playerService, assetService);
    Player player = new Player("abc");
    player.setId(PLAYER_ID);
    when(playerService.getCurrentPlayer()).thenReturn(player);

    instance.afterPropertiesSet();
  }

  @Test
  public void testGetPlayerAchievementsForCurrentUser() throws Exception {
    instance.playerAchievements.add(new PlayerAchievement());
    instance.getPlayerAchievements(PLAYER_ID);
    verify(fafService).addOnMessageListener(ArgumentMatchers.any(), ArgumentMatchers.any());
    verifyNoMoreInteractions(fafService);
  }

  @Test
  public void testGetPlayerAchievementsForAnotherUser() throws Exception {
    List<PlayerAchievement> achievements = Arrays.asList(new PlayerAchievement(), new PlayerAchievement());
    when(fafService.getPlayerAchievements(PLAYER_ID)).thenReturn(CompletableFuture.completedFuture(achievements));

    List<PlayerAchievement> playerAchievements = instance.getPlayerAchievements(PLAYER_ID).toCompletableFuture().get(5, TimeUnit.SECONDS);

    assertThat(playerAchievements, hasSize(2));
    assertThat(playerAchievements, is(achievements));
    verify(fafService).getPlayerAchievements(PLAYER_ID);
  }

  @Test
  public void testGetAchievementDefinitions() throws Exception {
    instance.getAchievementDefinitions();
    verify(fafService).getAchievementDefinitions();
  }

  @Test
  public void testGetHiddenThrowsUnsupportedOperationException() throws Exception {
    assertThrows(UnsupportedOperationException.class, () -> instance.getImage(null, HIDDEN));
  }

  @Test
  public void testLoadAndCacheImageRevealed() throws Exception {
    AchievementDefinition achievementDefinition = AchievementDefinitionBuilder.create().defaultValues().get();
    Path cacheSubDir = Paths.get("achievements").resolve(AchievementState.REVEALED.name().toLowerCase());
    instance.getImage(achievementDefinition, REVEALED);
    verify(assetService).loadAndCacheImage(new URL(achievementDefinition.getRevealedIconUrl()), cacheSubDir, null, 128, 128);
  }

  @Test
  public void testLoadAndCacheImageUnlocked() throws Exception {
    AchievementDefinition achievementDefinition = AchievementDefinitionBuilder.create().defaultValues().get();
    Path cacheSubDir = Paths.get("achievements").resolve(AchievementState.UNLOCKED.name().toLowerCase());
    instance.getImage(achievementDefinition, UNLOCKED);
    verify(assetService).loadAndCacheImage(new URL(achievementDefinition.getUnlockedIconUrl()), cacheSubDir, null, 128, 128);
  }
}
