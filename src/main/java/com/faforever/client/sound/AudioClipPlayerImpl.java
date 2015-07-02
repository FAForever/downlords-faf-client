package com.faforever.client.sound;

import javafx.scene.media.AudioClip;

public class AudioClipPlayerImpl implements AudioClipPlayer {

  @Override
  public void playSound(AudioClip audioClip) {
    audioClip.play();
  }
}
