package com.faforever.client.remote.domain.outbound.faf;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;


@EqualsAndHashCode(callSuper = true)
@Value
public class RemoveFriendMessage extends RemoveSocialMessage {
  int friend;
}
