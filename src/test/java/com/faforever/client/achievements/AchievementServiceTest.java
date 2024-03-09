package com.faforever.client.achievements;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.AchievementDefinitionBuilder;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.dto.AchievementDefinition;
import com.faforever.commons.api.dto.AchievementState;
import com.faforever.commons.api.dto.PlayerAchievement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AchievementServiceTest extends ServiceTest {

  private static final int PLAYER_ID = 123;

  @InjectMocks
  private AchievementService instance;
  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private AssetService assetService;
  @Mock
  private NotificationService notificationService;

  @BeforeEach
  public void setUp() throws Exception {

  }

  @Test
  public void testGetPlayerAchievementsForAnotherUser() throws Exception {
    List<PlayerAchievement> achievements = Arrays.asList(new PlayerAchievement(), new PlayerAchievement());
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.fromIterable(achievements));
    when(fafApiAccessor.getMaxPageSize()).thenReturn(10000);

    StepVerifier.create(instance.getPlayerAchievements(PLAYER_ID)).expectNextSequence(achievements).verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasPageSize(10000)));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("player.id").eq(PLAYER_ID))));
  }

  @Test
  public void testGetAchievementDefinitions() throws Exception {
    AchievementDefinition achievementDefinition = AchievementDefinitionBuilder.create().defaultValues().get();
    when(fafApiAccessor.getMaxPageSize()).thenReturn(10000);

    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(achievementDefinition));

    StepVerifier.create(instance.getAchievementDefinitions()).expectNext(achievementDefinition).verifyComplete();

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
    Path cacheSubDir = Path.of("achievements").resolve(AchievementState.REVEALED.name().toLowerCase());
    instance.getImage(achievementDefinition, AchievementState.REVEALED);
    verify(assetService).loadAndCacheImage(new URL(achievementDefinition.getRevealedIconUrl()), cacheSubDir, null, 128, 128);
  }

  @Test
  public void testLoadAndCacheImageUnlocked() throws Exception {
    AchievementDefinition achievementDefinition = AchievementDefinitionBuilder.create().defaultValues().get();
    Path cacheSubDir = Path.of("achievements").resolve(AchievementState.UNLOCKED.name().toLowerCase());
    instance.getImage(achievementDefinition, AchievementState.UNLOCKED);
    verify(assetService).loadAndCacheImage(new URL(achievementDefinition.getUnlockedIconUrl()), cacheSubDir, null, 128, 128);
  }
}
