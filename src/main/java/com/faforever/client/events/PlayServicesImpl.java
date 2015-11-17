package com.faforever.client.events;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.parsecom.CloudAccessor;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.JavaFxUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;

public class PlayServicesImpl implements PlayServices {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final ReentrantLock BATCH_REQUEST_LOCK = new ReentrantLock();
  private final ObservableList<PlayerAchievement> readOnlyPlayerAchievements;
  private final ObservableList<PlayerAchievement> playerAchievements;

  @Resource
  PreferencesService preferencesService;
  @Resource
  UserService userService;
  @Resource
  CloudAccessor cloudAccessor;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  @Resource
  FafApiAccessor fafApiAccessor;
  @Resource
  ExecutorService executorService;

  public PlayServicesImpl() {
    playerAchievements = FXCollections.observableArrayList();
    readOnlyPlayerAchievements = FXCollections.unmodifiableObservableList(playerAchievements);
  }

  @Override
  public CompletableFuture<Void> authorize() {
    return CompletableFuture.runAsync(() -> {
      fafApiAccessor.authorize(userService.getUid());
      loadCurrentPlayerAchievements();
    }, executorService);
  }

  @Override
  public ObservableList<PlayerAchievement> getPlayerAchievements(String username) {
    if (userService.getUsername().equals(username)) {
      return readOnlyPlayerAchievements;
    }

    ObservableList<PlayerAchievement> playerAchievements = FXCollections.observableArrayList();

    cloudAccessor.getPlayerIdForUsername(username)
        .<List<PlayerAchievement>>thenApply(playerId -> {
          if (StringUtils.isEmpty(playerId)) {
            return null;
          }
          return fafApiAccessor.getPlayerAchievements(Integer.parseInt(playerId));
        })
        .thenAccept(playerAchievements::setAll)
        .exceptionally(throwable -> {
          logger.warn("Could not load achievements for player: " + username, throwable);
          return null;
        });

    return playerAchievements;
  }

  @Override
  @Cacheable(CacheNames.ACHIEVEMENTS)
  public CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions() {
    // TODO make async again
    return CompletableFuture.completedFuture(fafApiAccessor.getAchievementDefinitions());
  }

  private void loadCurrentPlayerAchievements() {
    JavaFxUtil.assertBackgroundThread();
    playerAchievements.setAll(fafApiAccessor.getPlayerAchievements(userService.getUid()));
  }
}

