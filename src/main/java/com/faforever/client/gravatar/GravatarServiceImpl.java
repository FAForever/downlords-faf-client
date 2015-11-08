package com.faforever.client.gravatar;

import com.timgroup.jgravatar.Gravatar;
import com.timgroup.jgravatar.GravatarDefaultImage;
import com.timgroup.jgravatar.GravatarRating;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class GravatarServiceImpl implements GravatarService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Gravatar gravatar;

  public GravatarServiceImpl() {
    gravatar = new Gravatar(Gravatar.DEFAULT_SIZE, GravatarRating.GENERAL_AUDIENCES, GravatarDefaultImage.GRAVATAR_ICON);
  }

  @Override
  public Image getGravatar(String email) {
    String url = gravatar.getUrl(email);
    logger.debug("Loading gravatar from {}", url);
    return new Image(url, true);
  }
}
