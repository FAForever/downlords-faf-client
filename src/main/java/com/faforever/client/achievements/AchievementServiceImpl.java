package com.faforever.client.achievements;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.UpdatedAchievement;
import com.faforever.client.legacy.UpdatedAchievementsInfo;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.PlayerService;
import com.faforever.client.user.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  public AchievementServiceImpl() {
    playerAchievements = FXCollections.observableArrayList();
    readOnlyPlayerAchievements = FXCollections.unmodifiableObservableList(playerAchievements);
  }

  @Override
  public ObservableList<PlayerAchievement> getPlayerAchievements(String username) {
    if (userService.getUsername().equals(username)) {
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

  @PostConstruct
  void postConstruct() {
    lobbyServerAccessor.addOnUpdatedAchievementsInfoListener(this::onUpdatedAchievements);
  }

  private void onUpdatedAchievements(UpdatedAchievementsInfo updatedAchievementsInfo) {
    updatedAchievementsInfo.getUpdatedAchievements().stream()
        .filter(UpdatedAchievement::getNewlyUnlocked)
        .forEachOrdered(updatedAchievement -> getAchievementDefinition(updatedAchievement.getAchievementId())
            .thenAccept(this::notifyAboutUnlockedAchievement)
            .exceptionally(throwable -> {
              logger.warn("Could not get achievement definition for achievement: {}", updatedAchievement.getAchievementId());
              return null;
            })
        );
    playerAchievements.setAll(fafApiAccessor.getPlayerAchievements(userService.getUid()));
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
