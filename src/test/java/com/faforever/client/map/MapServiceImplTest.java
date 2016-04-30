package com.faforever.client.map;

import com.faforever.client.game.MapInfoBean;
import com.faforever.client.game.MapSize;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import javafx.collections.ObservableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class MapServiceImplTest {

  @Rule
  public TemporaryFolder customMapsDirectory = new TemporaryFolder();
  @Rule
  public TemporaryFolder gameDirectory = new TemporaryFolder();

  private MapServiceImpl instance;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private Preferences preferences;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;
  private Path mapsDirectory;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new MapServiceImpl();
    instance.preferencesService = preferencesService;

    mapsDirectory = gameDirectory.newFolder("maps").toPath();

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(forgedAlliancePrefs.getCustomMapsDirectory()).thenReturn(customMapsDirectory.getRoot().toPath());
    when(forgedAlliancePrefs.getPath()).thenReturn(gameDirectory.getRoot().toPath());
  }

  @Test
  public void testLoadSmallPreview() throws Exception {

  }

  @Test
  public void testLoadLargePreview() throws Exception {

  }

  @Test
  public void testReadMapVaultInBackground() throws Exception {

  }

  @Test
  public void testGetLocalMapsNoMaps() throws Exception {
    assertThat(instance.getLocalMaps(), hasSize(0));
  }

  @Test
  public void testGetLocalMapsOfficialMap() throws Exception {
    Path scmp001 = Files.createDirectory(mapsDirectory.resolve("SCMP_001"));
    Files.copy(getClass().getResourceAsStream("/maps/SCMP_001/SCMP_001_scenario.lua"), scmp001.resolve("SCMP_001_scenario.lua"));

    ObservableList<MapInfoBean> localMaps = instance.getLocalMaps();
    assertThat(localMaps, hasSize(1));

    MapInfoBean mapInfoBean = localMaps.get(0);
    assertThat(mapInfoBean, notNullValue());
    assertThat(mapInfoBean.getTechnicalName(), is("SCMP_001"));
    assertThat(mapInfoBean.getDisplayName(), is("Burial Mounds"));
    assertThat(mapInfoBean.getSize(), equalTo(MapSize.get(1024, 1024)));
  }

  @Test
  public void testGetMapInfoBeanLocallyFromName() throws Exception {

  }

  @Test
  public void testGetMapInfoBeanFromVaultByName() throws Exception {

  }

  @Test
  public void testIsOfficialMap() throws Exception {

  }

  @Test
  public void testIsAvailable() throws Exception {

  }

  @Test
  public void testDownload() throws Exception {

  }

  @Test
  public void testGetComments() throws Exception {

  }
}
