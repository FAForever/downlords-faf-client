package com.faforever.client.achievements;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.AchievementDefinitionBuilder;
import com.faforever.client.remote.AssetService;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.dto.AchievementDefinition;
import com.faforever.commons.api.dto.AchievementState;
import com.faforever.commons.api.dto.PlayerAchievement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Flux;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AchievementServiceTest extends ServiceTest {

  private static final int PLAYER_ID = 123;

  private AchievementService instance;
  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private AssetService assetService;

  @BeforeEach
  public void setUp() throws Exception {
    when(fafApiAccessor.getMaxPageSize()).thenReturn(10000);
    instance = new AchievementService(fafApiAccessor, assetService);
  }

  @Test
  public void testGetPlayerAchievementsForAnotherUser() throws Exception {
    List<PlayerAchievement> achievements = Arrays.asList(new PlayerAchievement(), new PlayerAchievement());
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.fromIterable(achievements));

    List<PlayerAchievement> playerAchievements = instance.getPlayerAchievements(PLAYER_ID).toCompletableFuture().get(5, TimeUnit.SECONDS);

    assertThat(playerAchievements, hasSize(2));
    assertThat(playerAchievements, is(achievements));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasPageSize(10000)));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("player.id").eq(PLAYER_ID))));
  }

  @Test
  public void testGetAchievementDefinitions() throws Exception {
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());
    instance.getAchievementDefinitions();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasPageSize(10000)));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasSort("order", true)));
  }

  @Test
  public void testGetHiddenThrowsUnsupportedOperationException() throws Exception {
    assertThrows(UnsupportedOperationException.class, () -> instance.getImage(null, AchievementState.HIDDEN));
  }

  @Test
  public void testLoadAndCacheImageRevealed() throws Exception {
    AchievementDefinition achievementDefinition = AchievementDefinitionBuilder.create().defaultValues().get();
    Path cacheSubDir = Paths.get("achievements").resolve(AchievementState.REVEALED.name().toLowerCase());
    instance.getImage(achievementDefinition, AchievementState.REVEALED);
    verify(assetService).loadAndCacheImage(new URL(achievementDefinition.getRevealedIconUrl()), cacheSubDir, null, 128, 128);
  }

  @Test
  public void testLoadAndCacheImageUnlocked() throws Exception {
    AchievementDefinition achievementDefinition = AchievementDefinitionBuilder.create().defaultValues().get();
    Path cacheSubDir = Paths.get("achievements").resolve(AchievementState.UNLOCKED.name().toLowerCase());
    instance.getImage(achievementDefinition, AchievementState.UNLOCKED);
    verify(assetService).loadAndCacheImage(new URL(achievementDefinition.getUnlockedIconUrl()), cacheSubDir, null, 128, 128);
  }
}
