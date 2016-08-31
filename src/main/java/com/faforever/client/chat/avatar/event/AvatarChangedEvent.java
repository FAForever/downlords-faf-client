package com.faforever.client.chat.avatar.event;

import com.faforever.client.chat.avatar.AvatarBean;
import org.jetbrains.annotations.Nullable;

public class AvatarChangedEvent {
  private final AvatarBean avatar;

  public AvatarChangedEvent(@Nullable AvatarBean avatarBean) {
    this.avatar = avatarBean;
  }

  @Nullable
  public AvatarBean getAvatar() {
    return avatar;
  }
}
