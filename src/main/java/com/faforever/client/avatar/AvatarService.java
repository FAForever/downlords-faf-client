package com.faforever.client.avatar;

import com.faforever.client.domain.api.Avatar;
import com.faforever.client.mapstruct.AvatarMapper;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.remote.FafServerAccessor;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.config.CacheNames.AVATARS;

@Lazy
@Service
@RequiredArgsConstructor
public class AvatarService {

  private final FafServerAccessor fafServerAccessor;
  private final AssetService assetService;
  private final PlayerService playerService;
  private final AvatarMapper avatarMapper;

  @Cacheable(value = AVATARS, sync = true)
  public Image loadAvatar(Avatar avatar) {
    if (avatar == null) {
      return null;
    }
    return assetService.loadAndCacheImage(avatar.url(), Path.of("avatars"));
  }

  public CompletableFuture<List<Avatar>> getAvailableAvatars() {
    return fafServerAccessor.getAvailableAvatars()
        .thenApply(dto -> avatarMapper.mapDtos(dto, new CycleAvoidingMappingContext()));
  }

  public void changeAvatar(Avatar avatar) {
    fafServerAccessor.selectAvatar(avatar.url());
    playerService.getCurrentPlayer().setAvatar(avatar);
  }
}
