package com.faforever.client.audio;

import javafx.scene.media.AudioClip;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class AudioClipPlayer {

  public void playSound(AudioClip audioClip) {
    audioClip.play();
  }
}
