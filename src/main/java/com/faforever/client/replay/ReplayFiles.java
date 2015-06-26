package com.faforever.client.replay;

import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.VictoryCondition;
import com.faforever.client.legacy.gson.GameAccessTypeAdapter;
import com.faforever.client.legacy.gson.GameStateTypeAdapter;
import com.faforever.client.legacy.gson.VictoryConditionTypeAdapter;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class ReplayFiles {

  public static final GsonBuilder GSON_BUILDER = new GsonBuilder()
      .disableHtmlEscaping()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .registerTypeAdapter(GameAccess.class, new GameAccessTypeAdapter())
      .registerTypeAdapter(GameState.class, new GameStateTypeAdapter())
      .registerTypeAdapter(VictoryCondition.class, new VictoryConditionTypeAdapter());

  private ReplayFiles() {
    // Utility class
  }

  public static Gson gson() {
    return GSON_BUILDER.create();
  }

  ;
}
