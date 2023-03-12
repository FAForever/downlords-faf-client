package com.faforever.client.main.event;

import java.nio.file.Path;

public record DeleteLocalReplayEvent(Path replayFile) {}
