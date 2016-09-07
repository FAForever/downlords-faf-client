package com.faforever.client.achievements;

import com.faforever.client.api.AchievementDefinition;
import com.faforever.client.audio.AudioController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationServiceImpl;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.UpdatedAchievement;
import com.faforever.client.remote.UpdatedAchievementsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;

public class AchievementUnlockedNotifier {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Resource
  NotificationServiceImpl notificationService;
  @Resource
  I18n i18n;
  @Resource
  AchievementService achievementService;
  @Resource
  FafService fafService;
  @Resource
  AudioController audioController;

  private long lastSoundPlayed;

  @PostConstruct
  void postConstruct() {
    fafService.addOnMessageListener(UpdatedAchievementsMessage.class, this::onUpdatedAchievementsMessage);
  }

  private void onUpdatedAchievementsMessage(UpdatedAchievementsMessage message) {
    message.getUpdatedAchievements().stream()
        .filter(UpdatedAchievement::getNewlyUnlocked)
        .forEachOrdered(updatedAchievement -> achievementService.getAchievementDefinition(updatedAchievement.getAchievementId())
            .thenAccept(this::notifyAboutUnlockedAchievement)
            .exceptionally(throwable -> {
              logger.warn("Could not get achievement definition for achievement: {}", updatedAchievement.getAchievementId(), throwable);
              return null;
            })
        );
  }

  private void notifyAboutUnlockedAchievement(AchievementDefinition achievementDefinition) {
    if (lastSoundPlayed < System.currentTimeMillis() - 1000) {
      audioController.playAchievementUnlockedSound();
      lastSoundPlayed = System.currentTimeMillis();
    }
    notificationService.addNotification(new TransientNotification(
            i18n.get("achievement.unlockedTitle"),
            achievementDefinition.getName(),
            achievementService.getRevealedIcon(achievementDefinition)
        )
    );
  }
}
