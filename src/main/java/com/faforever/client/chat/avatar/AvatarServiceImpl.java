package com.faforever.client.chat.avatar;

import com.faforever.client.remote.FafService;
import javafx.scene.image.Image;
import org.springframework.cache.annotation.Cacheable;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static com.faforever.client.config.CacheNames.AVATARS;

public class AvatarServiceImpl implements AvatarService {

  @Resource
  FafService fafService;

  @Override
  @Cacheable(AVATARS)
  public Image loadAvatar(String avatarUrl) {
    return new Image(avatarUrl, true);
  }

  @Override
  public CompletionStage<List<AvatarBean>> getAvailableAvatars() {
    return fafService.getAvailableAvatars();
  }

  @Override
  public void changeAvatar(AvatarBean avatar) {
    fafService.selectAvatar(avatar);
  }
}
