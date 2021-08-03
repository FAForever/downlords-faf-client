package com.faforever.client.achievements;

import com.faforever.client.audio.AudioService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.remote.FafService;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.dto.AchievementDefinition;
import com.faforever.commons.api.dto.AchievementType;
import com.faforever.commons.lobby.UpdatedAchievementsInfo;
import com.faforever.commons.lobby.UpdatedAchievementsInfo.AchievementState;
import com.faforever.commons.lobby.UpdatedAchievementsInfo.UpdatedAchievement;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class AchievementUnlockedNotifierTest extends ServiceTest {
  @Mock
  private AchievementUnlockedNotifier instance;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private AchievementService achievementService;
  @Mock
  private FafService fafService;
  @Mock
  private AudioService audioService;

  @Captor
  private ArgumentCaptor<Consumer<UpdatedAchievementsInfo>> listenerCaptor;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new AchievementUnlockedNotifier(notificationService, i18n, achievementService, fafService, audioService);
    instance.afterPropertiesSet();

    verify(fafService).addOnMessageListener(eq(UpdatedAchievementsInfo.class), listenerCaptor.capture());
  }

  @Test
  public void newlyUnlocked() throws Exception {
    AchievementDefinition achievementDefinition = new AchievementDefinition();
    achievementDefinition.setType(AchievementType.STANDARD);
    achievementDefinition.setName("Test Achievement");
    when(achievementService.getImage(achievementDefinition, AchievementState.UNLOCKED)).thenReturn(mock(Image.class));

    triggerUpdatedAchievementsMessage(achievementDefinition, true);

    verify(audioService).playAchievementUnlockedSound();

    ArgumentCaptor<TransientNotification> notificationCaptor = ArgumentCaptor.forClass(TransientNotification.class);
    verify(notificationService).addNotification(notificationCaptor.capture());

    TransientNotification notification = notificationCaptor.getValue();

    assertThat(notification.getImage(), notNullValue());
    assertThat(notification.getTitle(), is("Achievement unlocked"));
    assertThat(notification.getText(), is("Test Achievement"));
  }

  @Test
  public void alreadyUnlocked() {
    AchievementDefinition achievementDefinition = new AchievementDefinition();
    achievementDefinition.setType(AchievementType.STANDARD);
    achievementDefinition.setName("Test Achievement");
    triggerUpdatedAchievementsMessage(achievementDefinition, false);

    verifyZeroInteractions(audioService);
    verifyZeroInteractions(notificationService);
  }

  private void triggerUpdatedAchievementsMessage(AchievementDefinition achievementDefinition, boolean newlyUnlocked) {
    when(achievementService.getAchievementDefinition("1234")).thenReturn(CompletableFuture.completedFuture(achievementDefinition));

    when(i18n.get("achievement.unlockedTitle")).thenReturn("Achievement unlocked");
    when(achievementService.getImage(achievementDefinition, AchievementState.REVEALED)).thenReturn(mock(Image.class));

    UpdatedAchievement updatedAchievement = new UpdatedAchievement("1234", AchievementState.UNLOCKED, 0, newlyUnlocked);
    UpdatedAchievementsInfo message = new UpdatedAchievementsInfo(List.of(updatedAchievement));

    listenerCaptor.getValue().accept(message);
  }
}
