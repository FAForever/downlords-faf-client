package com.faforever.client.api;

import com.google.api.client.util.Key;

import java.util.*;
import java.util.Map;

public class History {

  @Key("history")
  private java.util.Map<String, List<Float>> data;

  public Map<String, List<Float>> getData() {
    return data;
  }
}
