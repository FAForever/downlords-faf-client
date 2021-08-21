package com.faforever.client.legacy;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Service
@Lazy
@Profile("testing")
public class MockUidService implements UidService {

  @Override
  public String generate(String sessionId, Path logFile) throws IOException {
    return "";
  }
}
