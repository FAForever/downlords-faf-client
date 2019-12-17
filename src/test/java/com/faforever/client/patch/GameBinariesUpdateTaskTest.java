package com.faforever.client.patch;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class GameBinariesUpdateTaskTest {
  @Rule
  public TemporaryFolder faDirectory = new TemporaryFolder();
  @Rule
  public TemporaryFolder fafBinDirectory = new TemporaryFolder();
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private PlatformService platformService;
  @Mock
  private I18n i18n;

  private GameBinariesUpdateTaskImpl instance;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new GameBinariesUpdateTaskImpl(i18n, preferencesService, platformService, new ClientProperties());

    Path faPath = faDirectory.getRoot().toPath();
    java.nio.file.Files.createDirectories(faPath.resolve("bin"));

    Preferences preferences = new Preferences();
    preferences.getForgedAlliance().setInstallationPath(faPath);

    when(preferencesService.getFafBinDirectory()).thenReturn(fafBinDirectory.getRoot().toPath());
    when(preferencesService.getPreferences()).thenReturn(preferences);
  }

  @Test(expected = IllegalStateException.class)
  public void testNoVersionThrowsException() throws Exception {
    instance.call();
  }

  @Test
  public void testCopyGameFilesToFafBinDirectory() throws Exception {
    Path fafBinPath = fafBinDirectory.getRoot().toPath();
    Path faBinPath = faDirectory.getRoot().toPath().resolve("bin");

    for (String fileName : GameBinariesUpdateTaskImpl.BINARIES_TO_COPY) {
      createFileWithSize(faBinPath.resolve(fileName), 1024);
    }
    createFileWithSize(faBinPath.resolve("splash.png"), 1024);

    instance.copyGameFilesToFafBinDirectory();

    List<Path> resultFiles = java.nio.file.Files.list(fafBinPath).collect(Collectors.toList());

    // Expected all files except splash.png to be copied
    assertThat(resultFiles.size(), is(GameBinariesUpdateTaskImpl.BINARIES_TO_COPY.size()));
    for (String fileName : GameBinariesUpdateTaskImpl.BINARIES_TO_COPY) {
      assertTrue(java.nio.file.Files.exists(fafBinPath.resolve(fileName)));
    }
  }

  @Test
  public void testUnixExecutableBitIsSet() throws Exception {
    Path faExePath = fafBinDirectory.newFile("ForgedAlliance.exe").toPath();
    instance.downloadFafExeIfNecessary(faExePath);
    Mockito.verify(platformService).setUnixExecutableAndWritableBits(faExePath);
  }

  private void createFileWithSize(Path file, int size) throws IOException {
    try (RandomAccessFile randomAccessFile = new RandomAccessFile(file.toFile(), "rw")) {
      randomAccessFile.setLength(size);
    }
  }
}
