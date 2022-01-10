package com.faforever.client.audio;

import com.faforever.client.test.ServiceTest;
import javafx.scene.media.AudioClip;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

public class AudioClipPlayerTest extends ServiceTest {

  @InjectMocks
  private AudioClipPlayer instance;

  @Test
  public void testPlaySound() throws Exception {
    AudioClip audioClip = new AudioClip(AudioClipPlayer.class.getResource("/theme/sounds/info.mp3").toURI().toURL().toString());

    // Not much to test here, since AudioClip is final. But let's at least call the method
    instance.playSound(audioClip);
  }
}
