package com.faforever.client.map;

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

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class DownloadMapTaskTest extends AbstractPlainJavaFxTest {

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
    instance = new DownloadMapTask();
    instance.preferencesService = preferencesService;
    instance.i18n = i18n;

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliance);
    when(forgedAlliance.getCustomMapsDirectory()).thenReturn(customMapsDirectory.getRoot().toPath());
  }

  @Test
  public void testCallWithoutMapUrlThrowsException() throws Exception {
    expectedException.expectMessage("mapUrl");
    expectedException.expect(NullPointerException.class);

    instance.setTechnicalMapName("");
    instance.call();
  }

  @Test
  public void testCallWithoutTechnicalMapNameThrowsException() throws Exception {
    expectedException.expectMessage("technicalMapName");
    expectedException.expect(NullPointerException.class);

    instance.setMapUrl("");
    instance.call();
  }

  @Test
  public void testCall() throws Exception {
    instance.setMapUrl(getClass().getResource("/maps/theta_passage_5.v0001.zip").toURI().toURL().toExternalForm());
    instance.setTechnicalMapName("");
    instance.call();

    assertTrue(Files.exists(customMapsDirectory.getRoot().toPath().resolve("theta_passage_5.v0001").resolve("theta_passage_5_scenario.lua")));
  }
}
