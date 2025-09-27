package com.faforever.client.fa.relay.gpg;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * GameState as sent by FA, ENDED was added by the FAF project
 */
public sealed interface GameState {

  String stateName();

  @RequiredArgsConstructor
  enum Known implements GameState {
    NONE("None"), IDLE("Idle"), LOBBY("Lobby"), LAUNCHING("Launching"), ENDED("Ended");

    private final static Map<String, GameState> KNOWN_MAP = Arrays.stream(values())
                                                                  .collect(Collectors.toMap(GameState::stateName,
                                                                                            Function.identity()));

    private final String stateName;

    @Override
    public String stateName() {
      return stateName;
    }
  }

  record Unknown(String stateName) implements GameState {}

  public static GameState fromName(String name) {
    if (Known.KNOWN_MAP.containsKey(name)) {
      return Known.KNOWN_MAP.get(name);
    }
    return new Unknown(name);
  }
}
