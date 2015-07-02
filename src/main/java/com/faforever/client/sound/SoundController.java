package com.faforever.client.sound;

import com.faforever.client.util.ThemeUtil;
import javafx.scene.media.AudioClip;

public interface SoundController {

  void playChatMentionSound();

  void playPrivateMessageSound();

  void playInfoNotificationSound();

  void playWarnNotificationSound();

  void playErrorNotificationSound();
}
