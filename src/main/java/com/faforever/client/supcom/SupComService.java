package com.faforever.client.supcom;

import java.io.IOException;
import java.util.List;

public interface SupComService {
  Process startGame(int uid, String mod, List<String> additionalArgs) throws IOException;
}
