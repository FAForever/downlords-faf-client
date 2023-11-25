package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.replay.ReplayMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class ReplayFileWriterImplTest extends ServiceTest {

  private static final String replayFileFormat = "%d-%s.fafreplay";
  private static final byte[] replayBytes = new byte[]{
      0x53, 0x75, 0x70, 0x72, 0x65, 0x6D, 0x65, 0x20, 0x43, 0x6F, 0x6D, 0x6D, 0x61, 0x6E, 0x64, 0x65,
      0x72, 0x20, 0x76, 0x31, 0x2E, 0x35, 0x30, 0x2E, 0x33, 0x35, 0x39, 0x39, 0x00, 0x0D, 0x0A, 0x00,
      0x52, 0x65, 0x70, 0x6C, 0x61, 0x79, 0x20, 0x76, 0x31, 0x2E, 0x39, 0x0D, 0x0A, 0x2F, 0x6D, 0x61,
      0x70, 0x73, 0x2F, 0x66, 0x6F, 0x72, 0x62, 0x69, 0x64, 0x64, 0x65, 0x6E, 0x20, 0x70, 0x61, 0x73,
      0x73, 0x2E, 0x76, 0x30, 0x30, 0x30, 0x31, 0x2F, 0x66, 0x6F, 0x72, 0x62, 0x69, 0x64, 0x64, 0x65,
      0x6E, 0x20, 0x70, 0x61, 0x73, 0x73, 0x2E, 0x73, 0x63, 0x6D, 0x61, 0x70, 0x00, 0x0D, 0x0A, 0x1A
  };
  private static final int UID = 1234;
  private static final String RECORDER = "Test";
  private static final String REPLAY_FILE_NAME = String.format(replayFileFormat, UID, RECORDER);

  @TempDir
  public Path tempDirectory;

  @Mock
  private I18n i81n;
  @Spy
  private ClientProperties clientProperties;
  @Mock
  private ByteArrayOutputStream replayData;
  @Mock
  private ClientProperties.Replay replay;
  @Spy
  private DataPrefs dataPrefs;

  @InjectMocks
  private ReplayFileWriterImpl instance;
  private Path replaysDirectory;

  @BeforeEach
  public void setUp() throws Exception {
    dataPrefs.setBaseDataDirectory(tempDirectory);

    Files.createDirectories(dataPrefs.getCacheDirectory());
    replaysDirectory = Files.createDirectories(dataPrefs.getReplaysDirectory());

    when(clientProperties.getReplay()).thenReturn(replay);
    when(replay.getReplayFileFormat()).thenReturn(replayFileFormat);
    when(i81n.getUserSpecificLocale()).thenReturn(Locale.US);
  }

  @Test
  public void writeReplayData() throws Exception {
    when(replayData.toByteArray()).thenReturn(replayBytes);
    ReplayMetadata replayInfo = new ReplayMetadata();
    replayInfo.setUid(UID);
    replayInfo.setRecorder(RECORDER);
    instance.writeReplayDataToFile(replayData, replayInfo);
    assertTrue(Files.exists(replaysDirectory.resolve(REPLAY_FILE_NAME)));
    Files.deleteIfExists(replaysDirectory.resolve(REPLAY_FILE_NAME));
  }
}

