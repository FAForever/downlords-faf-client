package com.faforever.client.replay;

import com.faforever.client.replay.Replay.GameOption;

import java.util.ArrayList;
import java.util.List;

public class GameOptionListBuilder {
  private final List<GameOption> gameOptions = new ArrayList<>();

  public static GameOptionListBuilder create() {
    return new GameOptionListBuilder();
  }

  public GameOptionListBuilder defaultValues() {
    append(new GameOption("Victory", "demoralization"));
    return this;
  }

  public GameOptionListBuilder append(GameOption gameOption) {
    gameOptions.add(gameOption);
    return this;
  }

  public GameOptionListBuilder replace(List<GameOption> gameOptions) {
    this.gameOptions.clear();
    this.gameOptions.addAll(gameOptions);
    return this;
  }

  public List<GameOption> get() {
    return gameOptions;
  }
}
