package com.faforever.client.map;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.UITest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class DownloadMapBeanTaskTest extends UITest {

  @TempDir
  public Path customMapsDirectory;

  private DownloadMapTask instance;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private I18n i18n;


  @BeforeEach
  public void setUp() throws Exception {
    Preferences preferences = PreferencesBuilder.create().defaultValues()
        .forgedAlliancePrefs()
        .customMapsDirectory(customMapsDirectory)
        .then()
        .get();

    instance = new DownloadMapTask(preferencesService, i18n);


    when(preferencesService.getPreferences()).thenReturn(preferences);
  }

  @Test
  public void testCallWithoutMapUrlThrowsException() throws Exception {
    instance.setFolderName("");
    assertEquals("mapUrl has not been set", assertThrows(NullPointerException.class, () -> instance.call()).getMessage());
  }

  @Test
  public void testCallWithoutFolderNameThrowsException() throws Exception {
    instance.setMapUrl(new URL("http://www.example.com"));
    assertEquals("folderName has not been set", assertThrows(NullPointerException.class, () -> instance.call()).getMessage());
  }

  @Test
  public void testCall() throws Exception {
    instance.setMapUrl(getClass().getResource("/maps/theta_passage_5.v0001.zip").toURI().toURL());
    instance.setFolderName("");
    instance.call();

    assertTrue(Files.exists(customMapsDirectory.resolve("theta_passage_5.v0001").resolve("theta_passage_5_scenario.lua")));
  }
}
