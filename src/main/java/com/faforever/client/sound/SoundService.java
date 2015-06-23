package com.faforever.client.sound;

import com.faforever.client.util.ThemeUtil;
import javafx.scene.media.AudioClip;

public interface SoundService {

  void playChatMentionSound();

  void playPrivateMessageSound();

  void playInfoNotificationSound();

  void playWarnNotificationSound();

  void playErrorNotificationSound();
}
