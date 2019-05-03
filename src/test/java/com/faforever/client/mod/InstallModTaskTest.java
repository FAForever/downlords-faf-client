package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class InstallModTaskTest extends AbstractPlainJavaFxTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder cacheDirectory = new TemporaryFolder();
  @Rule
  public TemporaryFolder modsDirectory = new TemporaryFolder();
  private InstallModTask instance;
  @Mock
  private I18n i18n;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private Preferences preferences;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;

  @Before
  public void setUp() throws Exception {
    instance = new InstallModTask(preferencesService, i18n);

    when(preferencesService.getCacheDirectory()).thenReturn(cacheDirectory.getRoot().toPath());
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(forgedAlliancePrefs.getModsDirectory()).thenReturn(modsDirectory.getRoot().toPath());
  }

  @Test
  public void testCallThrowsExceptionWhenUrlIsNotSet() throws Exception {
    expectedException.expectMessage("url");
    instance.call();
  }

  @Test
  public void testCall() throws Exception {
    Path modTargetDirectory = modsDirectory.getRoot().toPath().resolve("SuicideConfirmation");
    Path fileThatShouldBeDeletedByInstall = modTargetDirectory.resolve("fileThatShouldBeDeletedByInstall.file");
    Files.createDirectory(modTargetDirectory);
    Files.createFile(fileThatShouldBeDeletedByInstall);

    instance.setUrl(getClass().getResource("/mods/Suicide Confirmation.v0003.zip"));
    instance.call();

    assertThat(Files.exists(modTargetDirectory.resolve("mod_info.lua")), is(true));
    assertThat(Files.exists(fileThatShouldBeDeletedByInstall), is(false));
  }
}
