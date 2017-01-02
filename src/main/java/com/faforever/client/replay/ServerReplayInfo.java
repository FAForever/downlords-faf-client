package com.faforever.client.replay;

import lombok.Data;

@Data
public class ServerReplayInfo {

  private String map;
  private String name;
  private long end;
  private long start;
  private long duration;
  private String mod;
  private int id;
}
