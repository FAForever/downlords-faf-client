package com.faforever.client.discord;

import com.google.gson.JsonElement;
import lombok.Value;

@Value
public class DiscordSpectateSecret{
  private int gameId;
  private int playerId;
}
