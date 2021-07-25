package com.faforever.client.avatar.event;

import com.faforever.client.avatar.AvatarBean;
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
