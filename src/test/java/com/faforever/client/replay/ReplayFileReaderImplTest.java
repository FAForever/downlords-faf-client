package com.faforever.client.replay;


import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ReplayFileReaderImplTest extends ServiceTest {

  @TempDir
  public Path temporaryFolder;

  private ReplayFileReaderImpl instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new ReplayFileReaderImpl();
  }

  @Test
  public void readReplayData() throws Exception {
    Path tempFile = temporaryFolder.resolve("replay.tmp");
    try (InputStream inputStream = new BufferedInputStream(getClass().getResourceAsStream("/replay/test.fafreplay"))) {
      Files.copy(inputStream, tempFile);
    }
    assertThat(instance.parseReplay(tempFile).getData().length, is(197007));
  }
}
