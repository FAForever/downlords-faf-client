package com.faforever.client.avatar;

import com.faforever.client.avatar.event.AvatarChangedEvent;
import com.faforever.client.domain.AvatarBean;
import com.faforever.client.mapstruct.AvatarMapper;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.remote.AssetService;
import com.faforever.client.remote.FafServerAccessor;
import com.google.common.eventbus.EventBus;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
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
public class AvatarService implements InitializingBean {

  private final FafServerAccessor fafServerAccessor;
  private final AssetService assetService;
  private final EventBus eventBus;
  private final AvatarMapper avatarMapper;

  @Cacheable(value = AVATARS, sync = true)
  public Image loadAvatar(AvatarBean avatar) {
    return assetService.loadAndCacheImage(avatar.getUrl(), Path.of("avatars"), null);
  }

  public CompletableFuture<List<AvatarBean>> getAvailableAvatars() {
    return fafServerAccessor.getAvailableAvatars()
        .thenApply(dto -> avatarMapper.mapDtos(dto, new CycleAvoidingMappingContext()));
  }

  public void changeAvatar(AvatarBean avatar) {
    fafServerAccessor.selectAvatar(avatar.getUrl());
    eventBus.post(new AvatarChangedEvent(avatar));
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    eventBus.register(this);
  }
}
