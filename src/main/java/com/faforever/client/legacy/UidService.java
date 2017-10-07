package com.faforever.client.legacy;

import java.io.IOException;
import java.nio.file.Path;

public interface UidService {

  String generate(String sessionId, Path logFile) throws IOException;
}
