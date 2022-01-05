package com.faforever.client.main.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;

@RequiredArgsConstructor
public class DeleteLocalReplayEvent {
  @Getter
  private final Path replayFile;
}
