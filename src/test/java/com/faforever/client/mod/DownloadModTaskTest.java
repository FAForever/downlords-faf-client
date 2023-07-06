package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.test.PlatformTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Spy;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DownloadModTaskTest extends PlatformTest {

  @TempDir
  public Path tempDirectory;
  private Path modsDirectory;

  private DownloadModTask instance;
  @Mock
  private I18n i18n;

  @Spy
  private DataPrefs dataPrefs;
  @Spy
  private ForgedAlliancePrefs forgedAlliancePrefs;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new DownloadModTask(i18n, dataPrefs, forgedAlliancePrefs);
    dataPrefs.setBaseDataDirectory(tempDirectory);
    forgedAlliancePrefs.setVaultBaseDirectory(tempDirectory);

    Files.createDirectories(dataPrefs.getCacheDirectory());
    modsDirectory = Files.createDirectories(forgedAlliancePrefs.getModsDirectory());
  }

  @Test
  public void testCallThrowsExceptionWhenUrlIsNotSet() throws Exception {
    assertEquals("url has not been set", assertThrows(NullPointerException.class, () -> instance.call()).getMessage());
  }

  @Test
  public void testCall() throws Exception {
    Path modTargetDirectory = Files.createDirectories(modsDirectory.resolve("SuicideConfirmation"));
    Path fileThatShouldBeDeletedByInstall = modTargetDirectory.resolve("fileThatShouldBeDeletedByInstall.file");
    Files.createFile(fileThatShouldBeDeletedByInstall);

    instance.setUrl(getClass().getResource("/mods/Suicide Confirmation.v0003.zip"));
    instance.call();

    assertThat(Files.exists(modTargetDirectory.resolve("mod_info.lua")), is(true));
    assertThat(Files.exists(fileThatShouldBeDeletedByInstall), is(false));
  }
}
