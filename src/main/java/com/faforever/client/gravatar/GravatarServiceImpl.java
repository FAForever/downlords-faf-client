package com.faforever.client.gravatar;

import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class GravatarServiceImpl implements GravatarService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


  public GravatarServiceImpl() {
  }

  @Override
  public Image getGravatar(String email) {
    return null;
  }
}
