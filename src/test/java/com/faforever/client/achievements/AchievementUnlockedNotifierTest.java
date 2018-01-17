package com.faforever.client.achievements;

import com.faforever.client.api.dto.AchievementDefinition;
import com.faforever.client.api.dto.AchievementType;
import com.faforever.client.audio.AudioService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.notification.notificationEvents.ShowTransientNotificationEvent;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.UpdatedAchievement;
import com.faforever.client.remote.UpdatedAchievementsMessage;
import javafx.scene.image.Image;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.faforever.client.achievements.AchievementService.AchievementState.REVEALED;
import static com.faforever.client.achievements.AchievementService.AchievementState.UNLOCKED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class AchievementUnlockedNotifierTest {
  @Mock
  private AchievementUnlockedNotifier instance;
  @Mock
  private ApplicationEventPublisher applicationEventPublisher;
  @Mock
  private I18n i18n;
  @Mock
  private AchievementService achievementService;
  @Mock
  private FafService fafService;
  @Mock
  private AudioService audioService;

  @Captor
  private ArgumentCaptor<Consumer<UpdatedAchievementsMessage>> listenerCaptor;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new AchievementUnlockedNotifier(i18n, achievementService, fafService, audioService, applicationEventPublisher);
    instance.postConstruct();

    verify(fafService).addOnMessageListener(eq(UpdatedAchievementsMessage.class), listenerCaptor.capture());
  }

  @Test
  public void newlyUnlocked() throws Exception {
    AchievementDefinition achievementDefinition = new AchievementDefinition();
    achievementDefinition.setType(AchievementType.STANDARD);
    achievementDefinition.setName("Test Achievement");
    when(achievementService.getImage(achievementDefinition, UNLOCKED)).thenReturn(mock(Image.class));

    triggerUpdatedAchievementsMessage(achievementDefinition, true);

    verify(audioService).playAchievementUnlockedSound();

    ArgumentCaptor<ShowTransientNotificationEvent> notificationCaptor = ArgumentCaptor.forClass(ShowTransientNotificationEvent.class);
    verify(applicationEventPublisher).publishEvent(notificationCaptor.capture());

    TransientNotification notification = notificationCaptor.getValue().getNotification();

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
    verify(applicationEventPublisher, never()).publishEvent(ArgumentMatchers.any());
  }

  private void triggerUpdatedAchievementsMessage(AchievementDefinition achievementDefinition, boolean newlyUnlocked) {
    when(achievementService.getAchievementDefinition("1234")).thenReturn(CompletableFuture.completedFuture(achievementDefinition));

    when(i18n.get("achievement.unlockedTitle")).thenReturn("Achievement unlocked");
    when(achievementService.getImage(achievementDefinition, REVEALED)).thenReturn(mock(Image.class));

    UpdatedAchievementsMessage message = new UpdatedAchievementsMessage();
    UpdatedAchievement updatedAchievement = new UpdatedAchievement();
    updatedAchievement.setNewlyUnlocked(newlyUnlocked);
    updatedAchievement.setAchievementId("1234");
    message.setUpdatedAchievements(Collections.singletonList(updatedAchievement));

    listenerCaptor.getValue().accept(message);
  }
}
