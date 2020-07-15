package com.faforever.client.util;

import lombok.Value;

@Value
public class Tuple<T, U> {
  T first;
  U second;
}
