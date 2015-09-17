package com.faforever.client.legacy;

import java.nio.file.Path;

public interface UidService {

  String generate(String sessionId, Path logFile);
}
