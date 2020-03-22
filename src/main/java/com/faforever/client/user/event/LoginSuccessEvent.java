package com.faforever.client.user.event;

import com.faforever.client.api.dto.MeResult;
import lombok.Value;

@Value
public class LoginSuccessEvent {
  private MeResult user;
}
