package com.faforever.client.chat;

import com.faforever.client.legacy.message.Avatar;
import javafx.scene.image.Image;
import org.springframework.cache.annotation.Cacheable;

public class AvatarServiceImpl implements AvatarService {

  @Override
  @Cacheable("avatars")
  public Image loadAvatar(Avatar avatar) {
    return new Image(avatar.url, true);
  }
}
