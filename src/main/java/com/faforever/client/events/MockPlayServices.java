package com.faforever.client.events;

import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MockPlayServices implements PlayServices {

  @Resource
  NotificationService notificationService;

  @Override
  public CompletableFuture<Void> authorize() {
    notificationService.addNotification(
        new TransientNotification(
            "Test notification",
            "Some text.",
            new Image("/themes/default/images/default_achievement.png")
        ));
    notificationService.addNotification(
        new TransientNotification(
            "Test notification 2",
            "Some text 2.",
            new Image("/themes/default/images/default_achievement.png")
        ));
    notificationService.addNotification(
        new TransientNotification(
            "Test notification 3",
            "Some text 3.",
            new Image("/themes/default/images/default_achievement.png")
        ));
    notificationService.addNotification(
        new TransientNotification(
            "Test notification 4",
            "Some text 4.",
            new Image("/themes/default/images/default_achievement.png")
        ));
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public ObservableList<PlayerAchievement> getPlayerAchievements(String username) {
    return FXCollections.emptyObservableList();
  }

  @Override
  public CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions() {
    return CompletableFuture.completedFuture(FXCollections.<AchievementDefinition>emptyObservableList());
  }

}
