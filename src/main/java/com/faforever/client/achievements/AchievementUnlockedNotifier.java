package com.faforever.client.achievements;

import com.faforever.client.audio.AudioService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.remote.FafService;
import com.faforever.commons.api.dto.AchievementDefinition;
import com.faforever.commons.api.dto.AchievementState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Lazy
@Component
@RequiredArgsConstructor
public class AchievementUnlockedNotifier implements InitializingBean {

  private final NotificationService notificationService;
  private final I18n i18n;
  private final AchievementService achievementService;
  private final FafService fafService;
  private final AudioService audioService;

  private long lastSoundPlayed;

  @Override
  public void afterPropertiesSet() {
  }

  private void notifyAboutUnlockedAchievement(AchievementDefinition achievementDefinition) {
    if (lastSoundPlayed < System.currentTimeMillis() - 1000) {
      audioService.playAchievementUnlockedSound();
      lastSoundPlayed = System.currentTimeMillis();
    }
    notificationService.addNotification(new TransientNotification(
            i18n.get("achievement.unlockedTitle"),
            achievementDefinition.getName(),
        achievementService.getImage(achievementDefinition, AchievementState.UNLOCKED)
        )
    );
  }
}
