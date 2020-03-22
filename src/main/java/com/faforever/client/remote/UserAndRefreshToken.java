package com.faforever.client.remote;

import com.faforever.client.api.dto.MeResult;
import lombok.Value;

@Value
public class UserAndRefreshToken {
  String refreshToken;
  MeResult user;
}
