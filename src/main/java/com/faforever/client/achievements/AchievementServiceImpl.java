package com.faforever.client.achievements;

import com.faforever.client.api.AchievementDefinition;
import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.PlayerAchievement;
import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.config.CacheNames;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.UpdatedAchievementsMessage;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.user.UserService;
import com.google.common.base.Strings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.springframework.cache.annotation.Cacheable;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadPoolExecutor;

import static com.github.nocatch.NoCatch.noCatch;

public class AchievementServiceImpl implements AchievementService {

  private static final int ACHIEVEMENT_IMAGE_SIZE = 128;
  private final ObservableList<PlayerAchievement> readOnlyPlayerAchievements;
  private final ObservableList<PlayerAchievement> playerAchievements;

  @Resource
  UserService userService;
  @Resource
  FafApiAccessor fafApiAccessor;
  @Resource
  FafService fafService;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  @Resource
  PlayerService playerService;
  @Resource
  ThemeService themeService;
  @Resource
  ThreadPoolExecutor threadPoolExecutor;
  @Resource
  PreferencesService preferencesService;

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

    PlayerInfoBean playerForUsername = playerService.getPlayerForUsername(username);
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
    String url;
    switch (achievementState) {
      case REVEALED:
        url = achievementDefinition.getRevealedIconUrl();
        break;
      case UNLOCKED:
        url = achievementDefinition.getUnlockedIconUrl();
        break;
      default:
        throw new UnsupportedOperationException("Not yet implemented");
    }
    if (Strings.isNullOrEmpty(url)) {
      return themeService.getThemeImage(ThemeService.DEFAULT_ACHIEVEMENT_IMAGE);
    }
    String filename = url.substring(url.lastIndexOf('/') + 1);
    Path cachedPreviewPath = preferencesService.getCacheDirectory().resolve("achievements").resolve(achievementState.name().toLowerCase()).resolve(filename);
    if (Files.exists(cachedPreviewPath)) {
      url = noCatch(() -> cachedPreviewPath.toUri().toURL()).toExternalForm();
    }
    Image image = new Image(url, ACHIEVEMENT_IMAGE_SIZE, ACHIEVEMENT_IMAGE_SIZE, true, true, true);
    JavaFxUtil.persistImage(image, cachedPreviewPath, filename.substring(filename.lastIndexOf('.') + 1));
    return image;
  }

  private void reloadAchievements() {
    playerAchievements.setAll(fafApiAccessor.getPlayerAchievements(userService.getUid()));
  }

  @PostConstruct
  void postConstruct() {
    fafService.addOnMessageListener(UpdatedAchievementsMessage.class, updatedAchievementsMessage -> reloadAchievements());
  }
}
