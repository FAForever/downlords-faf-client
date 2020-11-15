package com.faforever.client.chat.avatar;

import com.faforever.client.remote.AssetService;
import com.faforever.client.remote.FafService;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.config.CacheNames.AVATARS;
import static com.github.nocatch.NoCatch.noCatch;

@Lazy
@Service
@RequiredArgsConstructor
public class AvatarServiceImpl implements AvatarService {

  private final FafService fafService;
  private final AssetService assetService;

  @Override
  @Cacheable(value = AVATARS, sync = true)
  public Image loadAvatar(String avatarUrl) {
    return assetService.loadAndCacheImage(noCatch(() -> new URL(avatarUrl)), Paths.get("avatars"), null);
  }

  @Override
  public CompletableFuture<List<AvatarBean>> getAvailableAvatars() {
    return fafService.getAvailableAvatars();
  }

  @Override
  public void changeAvatar(AvatarBean avatar) {
    fafService.selectAvatar(avatar);
  }
}
