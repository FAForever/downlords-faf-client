package com.faforever.client.patch;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class GameBinariesUpdateTaskTest extends ServiceTest {
  @TempDir
  public Path faDirectory;
  @TempDir
  public Path fafBinDirectory;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private PlatformService platformService;
  @Mock
  private I18n i18n;

  private GameBinariesUpdateTaskImpl instance;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new GameBinariesUpdateTaskImpl(i18n, preferencesService, platformService, new ClientProperties());

    Path faPath = faDirectory;
    Files.createDirectories(faPath.resolve("bin"));

    Preferences preferences = PreferencesBuilder.create().defaultValues()
        .forgedAlliancePrefs()
        .installationPath(faPath)
        .then()
        .get();

    when(preferencesService.getFafBinDirectory()).thenReturn(fafBinDirectory);
    when(preferencesService.getPreferences()).thenReturn(preferences);
  }

  @Test
  public void testNoVersionThrowsException() throws Exception {
    assertThrows(IllegalStateException.class, () -> instance.call());
  }

  @Test
  public void testCopyGameFilesToFafBinDirectory() throws Exception {
    Path fafBinPath = fafBinDirectory;
    Path faBinPath = Files.createDirectories(faDirectory.resolve("bin"));

    for (String fileName : GameBinariesUpdateTaskImpl.BINARIES_TO_COPY) {
      createFileWithSize(faBinPath.resolve(fileName), 1024);
    }
    createFileWithSize(faBinPath.resolve("splash.png"), 1024);

    instance.copyGameFilesToFafBinDirectory();

    List<Path> resultFiles = Files.list(fafBinPath).filter(file -> !file.toFile().isDirectory()).collect(Collectors.toList());

    // Expected all files except splash.png to be copied
    assertThat(resultFiles.size(), is(GameBinariesUpdateTaskImpl.BINARIES_TO_COPY.size()));
    for (String fileName : GameBinariesUpdateTaskImpl.BINARIES_TO_COPY) {
      assertTrue(java.nio.file.Files.exists(fafBinPath.resolve(fileName)));
    }
  }

  @Test
  public void testUnixExecutableBitIsSet() throws Exception {
    Path faExePath = Files.createFile(fafBinDirectory.resolve("ForgedAlliance.exe"));
    instance.downloadFafExeIfNecessary(faExePath);
    Mockito.verify(platformService).setUnixExecutableAndWritableBits(faExePath);
  }

  private void createFileWithSize(Path file, int size) throws IOException {
    Files.createFile(file);
    try (RandomAccessFile randomAccessFile = new RandomAccessFile(file.toFile(), "rw")) {
      randomAccessFile.setLength(size);
    }
  }
}
