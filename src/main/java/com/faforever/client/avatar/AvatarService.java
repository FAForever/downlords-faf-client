package com.faforever.client.avatar;

import javafx.scene.image.Image;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AvatarService {

  Image loadAvatar(String avatarUrl);

  CompletableFuture<List<AvatarBean>> getAvailableAvatars();

  void changeAvatar(AvatarBean avatar);
}
