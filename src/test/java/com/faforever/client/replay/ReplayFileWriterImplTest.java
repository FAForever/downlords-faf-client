package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class ReplayFileWriterImplTest {

  private static String replayFileFormat = "%d-%s.fafreplay";
  private static final byte[] replayBytes = new byte[]{
      0x53, 0x75, 0x70, 0x72, 0x65, 0x6D, 0x65, 0x20, 0x43, 0x6F, 0x6D, 0x6D, 0x61, 0x6E, 0x64, 0x65,
      0x72, 0x20, 0x76, 0x31, 0x2E, 0x35, 0x30, 0x2E, 0x33, 0x35, 0x39, 0x39, 0x00, 0x0D, 0x0A, 0x00,
      0x52, 0x65, 0x70, 0x6C, 0x61, 0x79, 0x20, 0x76, 0x31, 0x2E, 0x39, 0x0D, 0x0A, 0x2F, 0x6D, 0x61,
      0x70, 0x73, 0x2F, 0x66, 0x6F, 0x72, 0x62, 0x69, 0x64, 0x64, 0x65, 0x6E, 0x20, 0x70, 0x61, 0x73,
      0x73, 0x2E, 0x76, 0x30, 0x30, 0x30, 0x31, 0x2F, 0x66, 0x6F, 0x72, 0x62, 0x69, 0x64, 0x64, 0x65,
      0x6E, 0x20, 0x70, 0x61, 0x73, 0x73, 0x2E, 0x73, 0x63, 0x6D, 0x61, 0x70, 0x00, 0x0D, 0x0A, 0x1A
  };
  private static int uid = 1234;
  private static String recorder = "Test";
  private static String replayFileName = String.format(replayFileFormat, uid, recorder);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private PreferencesService preferencesService;

  @Mock
  private I18n i81n;

  @Mock
  private ClientProperties clientProperties;

  @Mock
  private ByteArrayOutputStream replayData;

  @Mock
  private ClientProperties.Replay replay;

  private ReplayFileWriterImpl instance;
  private LocalReplayInfo replayInfo;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new ReplayFileWriterImpl(i81n, clientProperties, preferencesService);
    when(clientProperties.getReplay()).thenReturn(replay);
    when(replay.getReplayFileFormat()).thenReturn(replayFileFormat);
    when(preferencesService.getReplaysDirectory()).thenReturn(Path.of("."));
    when(preferencesService.getCacheDirectory()).thenReturn(Path.of("."));
    when(i81n.getUserSpecificLocale()).thenReturn(Locale.US);
  }

  @Test
  public void writeReplayData() throws Exception {
    when(replayData.toByteArray()).thenReturn(replayBytes);
    replayInfo = new LocalReplayInfo();
    replayInfo.setUid(uid);
    replayInfo.setRecorder(recorder);
    instance.writeReplayDataToFile(replayData, replayInfo);
    assertTrue(Files.exists(Path.of(replayFileName)));
    Files.deleteIfExists(Path.of(replayFileName));
  }
}

