package com.faforever.client.chat;

import javafx.scene.image.Image;
import org.springframework.cache.annotation.Cacheable;

import static com.faforever.client.config.CacheNames.AVATARS;

public class AvatarServiceImpl implements AvatarService {

  @Override
  @Cacheable(AVATARS)
  public Image loadAvatar(String avatarUrl) {
    return new Image(avatarUrl, true);
  }
}
