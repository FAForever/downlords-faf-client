package com.faforever.client.chat;

import javafx.scene.image.Image;
import org.springframework.cache.annotation.Cacheable;

public class AvatarServiceImpl implements AvatarService {

  @Override
  @Cacheable("avatars")
  public Image loadAvatar(String avatarUrl) {
    return new Image(avatarUrl, true);
  }
}
