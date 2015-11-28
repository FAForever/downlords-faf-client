package com.faforever.client.achievements;

import com.faforever.client.ThemeService;
import com.faforever.client.api.AchievementDefinition;
import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.PlayerAchievement;
import com.faforever.client.config.CacheNames;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.UpdatedAchievement;
import com.faforever.client.legacy.UpdatedAchievementsMessageLobby;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.PlayerService;
import com.faforever.client.user.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AchievementServiceImpl implements AchievementService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final ObservableList<PlayerAchievement> readOnlyPlayerAchievements;
  private final ObservableList<PlayerAchievement> playerAchievements;

  @Resource
  UserService userService;
  @Resource
  FafApiAccessor fafApiAccessor;
  @Resource
  LobbyServerAccessor lobbyServerAccessor;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  @Resource
  PlayerService playerService;
  @Resource
  ThemeService themeService;

  public AchievementServiceImpl() {
    playerAchievements = FXCollections.observableArrayList();
    readOnlyPlayerAchievements = FXCollections.unmodifiableObservableList(playerAchievements);
  }

  @Override
  public ObservableList<PlayerAchievement> getPlayerAchievements(String username) {
    if (userService.getUsername().equals(username)) {
      if (readOnlyPlayerAchievements.isEmpty()) {
        updatePlayerAchievementsFromServer();
      }
      return readOnlyPlayerAchievements;
    }

    int playerId = playerService.getPlayerForUsername(username).getId();
    return FXCollections.observableList(fafApiAccessor.getPlayerAchievements(playerId));
  }

  @Override
  public CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions() {
    // TODO make async again
    return CompletableFuture.completedFuture(fafApiAccessor.getAchievementDefinitions());
  }

  @Override
  public CompletableFuture<AchievementDefinition> getAchievementDefinition(String achievementId) {
    return CompletableFuture.completedFuture(fafApiAccessor.getAchievementDefinition(achievementId));
  }

  @Override
  @Cacheable(CacheNames.ACHIEVEMENTS)
  public Image getRevealedIcon(AchievementDefinition achievementDefinition) {
    if (achievementDefinition.getRevealedIconUrl() == null) {
      return new Image(themeService.getThemeFile(ThemeService.DEFAULT_ACHIEVEMENT_IMAGE), true);
    }
    return new Image(achievementDefinition.getRevealedIconUrl(), true);
  }

  @Override
  @Cacheable(CacheNames.ACHIEVEMENTS)
  public Image getUnlockedIcon(AchievementDefinition achievementDefinition) {
    if (achievementDefinition.getUnlockedIconUrl() == null) {
      return new Image(themeService.getThemeFile(ThemeService.DEFAULT_ACHIEVEMENT_IMAGE), true);
    }
    return new Image(achievementDefinition.getUnlockedIconUrl(), true);
  }

  private void updatePlayerAchievementsFromServer() {
    playerAchievements.setAll(fafApiAccessor.getPlayerAchievements(userService.getUid()));
  }

  @PostConstruct
  void postConstruct() {
    lobbyServerAccessor.addOnUpdatedAchievementsInfoListener(this::onUpdatedAchievements);
  }

  private void onUpdatedAchievements(UpdatedAchievementsMessageLobby updatedAchievementsMessage) {
    updatedAchievementsMessage.getUpdatedAchievements().stream()
        .filter(UpdatedAchievement::getNewlyUnlocked)
        .forEachOrdered(updatedAchievement -> getAchievementDefinition(updatedAchievement.getAchievementId())
            .thenAccept(this::notifyAboutUnlockedAchievement)
            .exceptionally(throwable -> {
              logger.warn("Could not get achievement definition for achievement: {}", updatedAchievement.getAchievementId());
              return null;
            })
        );
    updatePlayerAchievementsFromServer();
  }

  private void notifyAboutUnlockedAchievement(AchievementDefinition achievementDefinition) {
    notificationService.addNotification(new TransientNotification(
            i18n.get("achievement.unlockedTitle"),
            achievementDefinition.getName(),
            new Image(achievementDefinition.getUnlockedIconUrl())
        )
    );
  }
}
