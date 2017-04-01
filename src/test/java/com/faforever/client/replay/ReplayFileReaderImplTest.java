package com.faforever.client.replay;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ReplayFileReaderImplTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private ReplayFileReaderImpl instance;

  @Before
  public void setUp() throws Exception {
    instance = new ReplayFileReaderImpl();
  }

  @Test
  public void readReplayData() throws Exception {
    Path tempFile = temporaryFolder.getRoot().toPath().resolve("replay.tmp");
    try (InputStream inputStream = new BufferedInputStream(getClass().getResourceAsStream("/replay/test.fafreplay"))) {
      Files.copy(inputStream, tempFile);
    }
    assertThat(instance.readRawReplayData(tempFile).length, is(197007));
  }
}
