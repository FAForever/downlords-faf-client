package com.faforever.client.audio;

import javafx.scene.media.AudioClip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AudioClipPlayerTest {

  private AudioClipPlayer instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new AudioClipPlayer();
  }

  @Test
  public void testPlaySound() throws Exception {
    AudioClip audioClip = new AudioClip(AudioClipPlayer.class.getResource("/theme/sounds/info.mp3").toURI().toURL().toString());

    // Not much to test here, since AudioClip is final. But let's at least call the method
    instance.playSound(audioClip);
  }
}
