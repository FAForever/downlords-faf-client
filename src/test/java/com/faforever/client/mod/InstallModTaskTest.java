package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesBuilder;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class InstallModTaskTest extends AbstractPlainJavaFxTest {

  @TempDir
  public Path cacheDirectory;
  @TempDir
  public Path modsDirectory;
  private InstallModTask instance;
  @Mock
  private I18n i18n;
  @Mock
  private PreferencesService preferencesService;

  @BeforeEach
  public void setUp() throws Exception {
    Preferences preferences = PreferencesBuilder.create().defaultValues()
        .forgedAlliancePrefs()
        .modsDirectory(modsDirectory)
        .then()
        .get();

    instance = new InstallModTask(preferencesService, i18n);

    when(preferencesService.getCacheDirectory()).thenReturn(cacheDirectory);
    when(preferencesService.getPreferences()).thenReturn(preferences);
  }

  @Test
  public void testCallThrowsExceptionWhenUrlIsNotSet() throws Exception {
    assertEquals("url has not been set", assertThrows(NullPointerException.class, () -> instance.call()).getMessage());
  }

  @Disabled("junit 5 does not yet support having multiple temp directories see https://github.com/junit-team/junit5/issues/1967")
  @Test
  public void testCall() throws Exception {
    Path modTargetDirectory = Files.createDirectories(modsDirectory.resolve("SuicideConfirmation"));
    Path fileThatShouldBeDeletedByInstall = modTargetDirectory.resolve("fileThatShouldBeDeletedByInstall.file");
    Files.createDirectory(modTargetDirectory);
    Files.createFile(fileThatShouldBeDeletedByInstall);

    instance.setUrl(getClass().getResource("/mods/Suicide Confirmation.v0003.zip"));
    instance.call();

    assertThat(Files.exists(modTargetDirectory.resolve("mod_info.lua")), is(true));
    assertThat(Files.exists(fileThatShouldBeDeletedByInstall), is(false));
  }
}
