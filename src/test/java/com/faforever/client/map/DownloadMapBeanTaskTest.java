package com.faforever.client.map;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URL;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DownloadMapBeanTaskTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder customMapsDirectory = new TemporaryFolder();

  private DownloadMapTask instance;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private I18n i18n;
  @Mock
  private Preferences preferences;
  @Mock
  private ForgedAlliancePrefs forgedAlliance;

  @Before
  public void setUp() throws Exception {
    instance = new DownloadMapTask(preferencesService, i18n);

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliance);
    when(forgedAlliance.getCustomMapsDirectory()).thenReturn(customMapsDirectory.getRoot().toPath());
  }

  @Test
  public void testCallWithoutMapUrlThrowsException() throws Exception {
    expectedException.expectMessage("mapUrl");
    expectedException.expect(NullPointerException.class);

    instance.setFolderName("");
    instance.call();
  }

  @Test
  public void testCallWithoutFolderNameThrowsException() throws Exception {
    expectedException.expectMessage("folderName");
    expectedException.expect(NullPointerException.class);

    instance.setMapUrl(new URL("http://www.example.com"));
    instance.call();
  }

  @Test
  public void testCall() throws Exception {
    instance.setMapUrl(getClass().getResource("/maps/theta_passage_5.v0001.zip").toURI().toURL());
    instance.setFolderName("");
    instance.call();

    assertTrue(Files.exists(customMapsDirectory.getRoot().toPath().resolve("theta_passage_5.v0001").resolve("theta_passage_5_scenario.lua")));
  }
}
