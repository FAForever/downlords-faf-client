package com.faforever.client.gravatar;

import com.google.common.hash.Hashing;
import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GravatarServiceImpl implements GravatarService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int DEFAULT_SIZE = 80;

  @Resource
  Environment environment;

  @Override
  public Image getGravatar(String email) {
    if (email == null) {
      return null;
    }
    String url = getUrl(email, DEFAULT_SIZE);
    logger.debug("Loading gravatar from {}", url);
    return new Image(url, true);
  }

  @Override
  public String getProfileUrl(String email) {
    return String.format(environment.getProperty("gravatar.profile.urlFormat"), hash(email));
  }

  private String getUrl(String email, int size) {
    return String.format(environment.getProperty("gravatar.avatar.urlFormat"), hash(email), size);
  }

  @NotNull
  private String hash(String email) {
    return Hashing.md5().hashString(email.toLowerCase().trim(), UTF_8).toString();
  }
}
