package com.faforever.client.audio;

import javafx.scene.media.AudioClip;

public class AudioClipPlayerImpl implements AudioClipPlayer {

  @Override
  public void playSound(AudioClip audioClip) {
    audioClip.play();
  }
}
