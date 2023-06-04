package com.faforever.client.map;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.test.UITest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Spy;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DownloadMapBeanTaskTest extends UITest {

  @TempDir
  public Path tempDirectory;

  private DownloadMapTask instance;


  @Mock
  private I18n i18n;
  @Spy
  private ForgedAlliancePrefs forgedAlliancePrefs;

  private Path mapsDirectory;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new DownloadMapTask(i18n, forgedAlliancePrefs);
    forgedAlliancePrefs.setVaultBaseDirectory(tempDirectory);
    mapsDirectory = Files.createDirectory(tempDirectory.resolve("maps"));
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

    assertTrue(Files.exists(mapsDirectory.resolve("theta_passage_5.v0001").resolve("theta_passage_5_scenario.lua")));
  }
}
