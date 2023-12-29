package com.faforever.client.achievements;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.remote.AssetService;
import com.faforever.commons.api.dto.AchievementDefinition;
import com.faforever.commons.api.dto.AchievementState;
import com.faforever.commons.api.dto.PlayerAchievement;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;


@Lazy
@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementService {

  private static final int ACHIEVEMENT_IMAGE_SIZE = 128;

  private final FafApiAccessor fafApiAccessor;
  private final AssetService assetService;
  private final NotificationService notificationService;

  @Cacheable(value = CacheNames.ACHIEVEMENTS, sync = true)
  public CompletableFuture<List<PlayerAchievement>> getPlayerAchievements(Integer playerId) {
    ElideNavigatorOnCollection<PlayerAchievement> navigator = ElideNavigator.of(PlayerAchievement.class)
        .collection()
        .setFilter(qBuilder().intNum("player.id").eq(playerId))
        .pageSize(fafApiAccessor.getMaxPageSize());
    return fafApiAccessor.getMany(navigator).collectList().toFuture();
  }


  @Cacheable(value = CacheNames.ACHIEVEMENTS, sync = true)
  public CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions() {
    ElideNavigatorOnCollection<AchievementDefinition> navigator = ElideNavigator.of(AchievementDefinition.class)
        .collection()
        .addSortingRule("order", true)
        .pageSize(fafApiAccessor.getMaxPageSize());
    return fafApiAccessor.getMany(navigator)
        .collectList()
        .toFuture();
  }

  @Cacheable(value = CacheNames.ACHIEVEMENT_IMAGES, sync = true)
  public Image getImage(AchievementDefinition achievementDefinition, AchievementState achievementState) {
    try {
      URL url = switch (achievementState) {
        case REVEALED -> new URL(achievementDefinition.getRevealedIconUrl());
        case UNLOCKED -> new URL(achievementDefinition.getUnlockedIconUrl());
        default -> throw new UnsupportedOperationException("Not yet implemented");
      };
      return assetService.loadAndCacheImage(url, Path.of("achievements").resolve(achievementState.name().toLowerCase()),
          null, ACHIEVEMENT_IMAGE_SIZE, ACHIEVEMENT_IMAGE_SIZE);
    } catch (MalformedURLException e) {
      log.warn("Could not load achievement image bad url for achievement: {}", achievementDefinition.getName(), e);
      notificationService.addPersistentErrorNotification("achievements.load.badUrl", achievementDefinition.getName());
      return null;
    }
  }
}
