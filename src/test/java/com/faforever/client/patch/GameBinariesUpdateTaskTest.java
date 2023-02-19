package com.faforever.client.patch;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

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

public class GameBinariesUpdateTaskTest extends ServiceTest {
  @TempDir
  public Path tempDirectory;
  private Path fafBinDirectory;
  private Path faDirectory;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private PlatformService platformService;
  @Mock
  private I18n i18n;
  @Spy
  private ClientProperties clientProperties;
  @Spy
  private DataPrefs dataPrefs;
  @Spy
  private ForgedAlliancePrefs forgedAlliancePrefs;

  @InjectMocks
  private GameBinariesUpdateTaskImpl instance;

  @BeforeEach
  public void setUp() throws Exception {
    Path faPath = tempDirectory.resolve("fa");

    dataPrefs.setBaseDataDirectory(tempDirectory);
    forgedAlliancePrefs.setInstallationPath(faPath);

    faDirectory = Files.createDirectories(faPath.resolve("bin"));
    fafBinDirectory = Files.createDirectories(dataPrefs.getBinDirectory());
  }

  @Test
  public void testNoVersionThrowsException() throws Exception {
    assertThrows(IllegalStateException.class, () -> instance.call());
  }

  @Test
  public void testCopyGameFilesToFafBinDirectory() throws Exception {
    Path faBinPath = Files.createDirectories(faDirectory);

    for (String fileName : GameBinariesUpdateTaskImpl.BINARIES_TO_COPY) {
      createFileWithSize(faBinPath.resolve(fileName), 1024);
    }
    createFileWithSize(faBinPath.resolve("splash.png"), 1024);

    instance.copyGameFilesToFafBinDirectory();

    List<Path> resultFiles = Files.list(fafBinDirectory).filter(file -> !file.toFile().isDirectory()).collect(Collectors.toList());

    // Expected all files except splash.png to be copied
    assertThat(resultFiles.size(), is(GameBinariesUpdateTaskImpl.BINARIES_TO_COPY.size()));
    for (String fileName : GameBinariesUpdateTaskImpl.BINARIES_TO_COPY) {
      assertTrue(Files.exists(fafBinDirectory.resolve(fileName)));
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
