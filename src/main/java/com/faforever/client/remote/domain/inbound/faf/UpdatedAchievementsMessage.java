package com.faforever.client.remote.domain.inbound.faf;

import com.faforever.client.remote.UpdatedAchievement;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Value
public class UpdatedAchievementsMessage extends FafInboundMessage {
  public static final String COMMAND = "updated_achievements";

  List<UpdatedAchievement> updatedAchievements;
}
