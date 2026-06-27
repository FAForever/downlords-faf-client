package com.faforever.client.avatar;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.mapstruct.AvatarMapper;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.AssetService;
import com.faforever.commons.api.dto.AvatarAssignment;
import com.faforever.commons.api.dto.Player;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import com.faforever.commons.api.elide.ElideNavigatorOnId;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
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

  public CompletableFuture<Avatar> getCurrentAvatar() {
    String playerId = String.valueOf(playerService.getCurrentPlayer().getId());
    ElideNavigatorOnId<Player> navigator = ElideNavigator.of(Player.class)
        .id(playerId)
        .addInclude("currentAvatar");
    return fafApiAccessor.getOne(navigator)
        .mapNotNull(Player::getCurrentAvatar)
        .map(avatarMapper::map)
        .toFuture();
  }

  public void changeAvatar(Avatar avatar) {
    String playerId = String.valueOf(playerService.getCurrentPlayer().getId());
    String avatarId = avatar == null || avatar.id() == null ? null : String.valueOf(avatar.id());
    fafApiAccessor.patchToOneRelationship("player", playerId, "currentAvatar", "avatar", avatarId).subscribe();
    playerService.getCurrentPlayer().setAvatar(avatar);
  }
}
