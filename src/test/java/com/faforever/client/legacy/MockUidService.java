package com.faforever.client.legacy;

import com.faforever.client.io.UidService;
import com.faforever.client.os.OperatingSystem;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Lazy
@Profile("testing")
public class MockUidService extends UidService {

  public MockUidService(OperatingSystem operatingSystem) {
    super(operatingSystem);
  }

  @Override
  public String generate(String sessionId) throws IOException {
    return "";
  }
}
