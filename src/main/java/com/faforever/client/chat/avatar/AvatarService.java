package com.faforever.client.chat.avatar;

import javafx.scene.image.Image;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface AvatarService {

  Image loadAvatar(String avatarUrl);

  CompletionStage<List<AvatarBean>> getAvailableAvatars();

  void changeAvatar(AvatarBean avatar);
}
