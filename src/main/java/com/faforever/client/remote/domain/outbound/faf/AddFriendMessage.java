package com.faforever.client.remote.domain.outbound.faf;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;


@EqualsAndHashCode(callSuper = true)
@Value
public class AddFriendMessage extends AddSocialMessage {
  int friend;
}
