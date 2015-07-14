package com.faforever.client.sound;

import javafx.scene.media.AudioClip;
import org.junit.Before;
import org.junit.Test;

public class AudioClipPlayerImplTest {

  private AudioClipPlayerImpl instance;

  @Before
  public void setUp() throws Exception {
    instance = new AudioClipPlayerImpl();
  }

  @Test
  public void testPlaySound() throws Exception {
    AudioClip audioClip = new AudioClip(AudioClipPlayerImpl.class.getResource("/themes/default/sounds/warn.mp3").toURI().toURL().toString());

    // Not much to test here, since AudioClip is final. But let's at least call the method
    instance.playSound(audioClip);
  }
}
