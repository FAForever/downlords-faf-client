package com.faforever.client.sound;

import com.faforever.client.main.MainController;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.media.AudioClip;
import javafx.stage.Stage;
import org.junit.Test;


import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class SoundControllerImplTest extends AbstractPlainJavaFxTest {
  private SoundControllerImpl instance;
  private Preferences preferences;
  private ChatPrefs chatPrefs;
  private NotificationsPrefs notificationsPrefs;

  @Override
  public void start(Stage stage) throws Exception {
    instance = new SoundControllerImpl();
    instance.mainController = mock(MainController.class);
    instance.preferencesService = mock(PreferencesService.class);
    instance.audioClipPlayer = mock(AudioClipPlayer.class);

    preferences = mock(Preferences.class);
    chatPrefs = mock(ChatPrefs.class);
    notificationsPrefs = mock(NotificationsPrefs.class);


    when(notificationsPrefs.soundsEnabledProperty()).thenReturn(new SimpleBooleanProperty());
    when(instance.mainController.getRoot()).thenReturn(getRoot());
    when(preferences.getNotification()).thenReturn(notificationsPrefs);
    when(preferences.getChat()).thenReturn(chatPrefs);
    when(preferences.getTheme()).thenReturn("default");
    when(instance.preferencesService.getPreferences()).thenReturn(preferences);

    instance.postConstruct();

    super.start(stage);
  }

  @Test
  public void testPlayChatMentionSound() throws Exception {
    when(notificationsPrefs.getSoundsEnabled()).thenReturn(true);
    when(notificationsPrefs.getMentionSoundEnabled()).thenReturn(true);

    instance.playChatMentionSound();

    verify(instance.audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayPrivateMessageSound() throws Exception {
    when(notificationsPrefs.getSoundsEnabled()).thenReturn(true);
    when(notificationsPrefs.getPrivateMessageSoundEnabled()).thenReturn(true);

    instance.playPrivateMessageSound();

    verify(instance.audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayInfoNotificationSound() throws Exception {
    when(notificationsPrefs.getSoundsEnabled()).thenReturn(true);
    when(notificationsPrefs.getInfoSoundEnabled()).thenReturn(true);

    instance.playInfoNotificationSound();

    verify(instance.audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayWarnNotificationSound() throws Exception {
    when(notificationsPrefs.getSoundsEnabled()).thenReturn(true);
    when(notificationsPrefs.getWarnSoundEnabled()).thenReturn(true);

    instance.playWarnNotificationSound();

    verify(instance.audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayErrorNotificationSound() throws Exception {
    when(notificationsPrefs.getSoundsEnabled()).thenReturn(true);
    when(notificationsPrefs.getErrorSoundEnabled()).thenReturn(true);

    instance.playErrorNotificationSound();

    verify(instance.audioClipPlayer).playSound(any(AudioClip.class));
  }
}