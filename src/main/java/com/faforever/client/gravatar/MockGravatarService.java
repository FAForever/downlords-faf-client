package com.faforever.client.gravatar;

import javafx.scene.image.Image;

public class MockGravatarService implements GravatarService {

  @Override
  public Image getGravatar(String email) {
    return new Image("http://example.com");
  }

  @Override
  public String getProfileUrl(String email) {
    return "";
  }
}
