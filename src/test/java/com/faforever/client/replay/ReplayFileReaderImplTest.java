package com.faforever.client.replay;

import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;

public class ReplayFileReaderImplTest {
  private ReplayFileReaderImpl instance;

  @Before
  public void setUp() throws Exception {
    instance = new ReplayFileReaderImpl();
  }

  @Test
  public void readReplayData() throws Exception {
    instance.readRawReplayData(Paths.get("."));
  }
}
