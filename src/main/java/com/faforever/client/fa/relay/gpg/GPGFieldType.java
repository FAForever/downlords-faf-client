package com.faforever.client.fa.relay.gpg;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public sealed interface GPGFieldType {

  int id();

  @RequiredArgsConstructor
  enum Known implements GPGFieldType {
    INT(0), STRING(1);

    private final static Map<Integer, Known> ID_TO_TYPE = Arrays.stream(values())
                                                                .collect(Collectors.toMap(GPGFieldType.Known::id,
                                                                                          Function.identity()));

    private final int id;

    @Override
    public int id() {
      return id;
    }
  }

  record Unknown(int id) implements GPGFieldType {}

  static GPGFieldType fromId(int id) {
    if (Known.ID_TO_TYPE.containsKey(id)) {
      return Known.ID_TO_TYPE.get(id);
    }

    return new Unknown(id);
  }
}
