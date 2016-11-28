package com.faforever.client.achievements;

import com.faforever.client.api.AchievementDefinition;
import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.PlayerAchievement;
import com.faforever.client.config.CacheNames;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.UpdatedAchievementsMessage;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadPoolExecutor;

import static com.github.nocatch.NoCatch.noCatch;


@Lazy
@Service
public class AchievementServiceImpl implements AchievementService {

  private static final int ACHIEVEMENT_IMAGE_SIZE = 128;
  private final ObservableList<PlayerAchievement> readOnlyPlayerAchievements;
  private final ObservableList<PlayerAchievement> playerAchievements;

  @Inject
  UserService userService;
  @Inject
  FafApiAccessor fafApiAccessor;
  @Inject
  FafService fafService;
  @Inject
  NotificationService notificationService;
  @Inject
  I18n i18n;
  @Inject
  PlayerService playerService;
  @Inject
  UiService uiService;
  @Inject
  ThreadPoolExecutor threadPoolExecutor;
  @Inject
  AssetService assetService;

  public AchievementServiceImpl() {
    playerAchievements = FXCollections.observableArrayList();
    readOnlyPlayerAchievements = FXCollections.unmodifiableObservableList(playerAchievements);
  }

  @Override
  public CompletionStage<List<PlayerAchievement>> getPlayerAchievements(String username) {
    if (userService.getUsername().equalsIgnoreCase(username)) {
      if (readOnlyPlayerAchievements.isEmpty()) {
        reloadAchievements();
      }
      return CompletableFuture.completedFuture(readOnlyPlayerAchievements);
    }

    Player playerForUsername = playerService.getPlayerForUsername(username);
    if (playerForUsername == null) {
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
    int playerId = playerForUsername.getId();
    return CompletableFuture.supplyAsync(() -> FXCollections.observableList(fafApiAccessor.getPlayerAchievements(playerId)), threadPoolExecutor);
  }

  @Override
  public CompletionStage<List<AchievementDefinition>> getAchievementDefinitions() {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getAchievementDefinitions(), threadPoolExecutor);
  }

  @Override
  public CompletionStage<AchievementDefinition> getAchievementDefinition(String achievementId) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getAchievementDefinition(achievementId), threadPoolExecutor);
  }

  @Override
  @Cacheable(CacheNames.ACHIEVEMENT_IMAGES)
  public Image getImage(AchievementDefinition achievementDefinition, AchievementState achievementState) {
    URL url;
    switch (achievementState) {
      case REVEALED:
        url = noCatch(() -> new URL(achievementDefinition.getRevealedIconUrl()));
        break;
      case UNLOCKED:
        url = noCatch(() -> new URL(achievementDefinition.getUnlockedIconUrl()));
        break;
      default:
        throw new UnsupportedOperationException("Not yet implemented");
    }
    return assetService.loadAndCacheImage(url, Paths.get("achievements").resolve(achievementState.name().toLowerCase()),
        null, ACHIEVEMENT_IMAGE_SIZE, ACHIEVEMENT_IMAGE_SIZE);
  }

  private void reloadAchievements() {
    playerAchievements.setAll(fafApiAccessor.getPlayerAchievements(userService.getUserId()));
  }

  @PostConstruct
  void postConstruct() {
    fafService.addOnMessageListener(UpdatedAchievementsMessage.class, updatedAchievementsMessage -> reloadAchievements());
  }
}
