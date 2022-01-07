package com.faforever.client.main.event;

import lombok.Value;

import java.nio.file.Path;

@Value
public class DeleteLocalReplayEvent {
  Path replayFile;
}
