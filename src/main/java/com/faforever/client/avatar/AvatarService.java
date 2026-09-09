package com.faforever.client.avatar;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.mapstruct.AvatarMapper;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.AssetService;
import com.faforever.commons.api.dto.AvatarAssignment;
import com.faforever.commons.api.dto.Player;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.config.CacheNames.AVATARS;
import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;

@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class AvatarService {

  private final FafApiAccessor fafApiAccessor;
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
    Integer playerId = playerService.getCurrentPlayer().getId();
    ElideNavigatorOnCollection<AvatarAssignment> navigator = ElideNavigator.of(AvatarAssignment.class)
        .collection()
        .setFilter(qBuilder().string("player.id").eq(String.valueOf(playerId)));
    return fafApiAccessor.getAll(navigator)
        .map(AvatarAssignment::getAvatar)
        .map(avatarMapper::map)
        .collectList()
        .toFuture();
  }

  public void changeAvatar(Avatar avatar) {
    // We don't update the local player here: the API writes login.avatar_id, the lobby server consumes the
    // resulting event and broadcasts a player_info back to us, which refreshes the avatar authoritatively.
    if (avatar == null || avatar.id() == null) {
      removeCurrentAvatar();
    } else {
      selectAvatar(avatar);
    }
  }

  private void selectAvatar(Avatar avatar) {
    String playerId = String.valueOf(playerService.getCurrentPlayer().getId());
    com.faforever.commons.api.dto.Avatar avatarDto = new com.faforever.commons.api.dto.Avatar();
    avatarDto.setId(String.valueOf(avatar.id()));
    Player playerDto = new Player();
    playerDto.setId(playerId);
    playerDto.setCurrentAvatar(avatarDto);
    fafApiAccessor.patch(ElideNavigator.of(Player.class).id(playerId), playerDto)
        .subscribe(null, throwable -> log.error("Could not select avatar ''{}''", avatar.id(), throwable));
  }

  private void removeCurrentAvatar() {
    String playerId = String.valueOf(playerService.getCurrentPlayer().getId());
    Avatar currentAvatar = playerService.getCurrentPlayer().getAvatar();

    if (currentAvatar == null || currentAvatar.id() == null) {
      return;
    }

    fafApiAccessor.deleteFromRelationship(ElideNavigator.of(Player.class).id(playerId)
            .relationshipLink(com.faforever.commons.api.dto.Avatar.class, "currentAvatar"),
            String.valueOf(currentAvatar.id()))
        .subscribe(null, throwable -> log.error("Could not remove current avatar", throwable));
  }
}
