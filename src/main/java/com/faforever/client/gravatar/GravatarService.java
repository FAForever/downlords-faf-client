package com.faforever.client.gravatar;

import com.faforever.client.config.CacheNames;
import javafx.scene.image.Image;
import org.springframework.cache.annotation.Cacheable;

public interface GravatarService {

  @Cacheable(CacheNames.GRAVATAR)
  Image getGravatar(String email);

  String getProfileUrl(String email);
}
