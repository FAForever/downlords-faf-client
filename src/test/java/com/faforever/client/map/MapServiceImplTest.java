package com.faforever.client.map;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.ThemeService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.luaj.vm2.LuaError;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ThreadPoolExecutor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapServiceImplTest extends AbstractPlainJavaFxTest {

  @Rule
  public TemporaryFolder cacheDirectory = new TemporaryFolder();
  @Rule
  public TemporaryFolder customMapsDirectory = new TemporaryFolder();
  @Rule
  public TemporaryFolder gameDirectory = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MapServiceImpl instance;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private Preferences preferences;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Mock
  private ObjectProperty<Path> customMapsDirectoryProperty;
  @Mock
  private ThreadPoolExecutor threadPoolExecutor;
  @Mock
  private TaskService taskService;
  @Mock
  private I18n i18n;
  @Mock
  private ThemeService themeService;
  @Mock
  private AssetService assetService;

  private Path mapsDirectory;

  @Before
  public void setUp() throws Exception {
    instance = new MapServiceImpl();
    instance.preferencesService = preferencesService;
    instance.threadPoolExecutor = threadPoolExecutor;
    instance.taskService = taskService;
    instance.directory = new RAMDirectory();
    instance.analyzer = new SimpleAnalyzer();
    instance.i18n = i18n;
    instance.themeService = themeService;
    instance.assetService = assetService;
    instance.mapPreviewUrlFormat = "http://127.0.0.1:65534/preview/%s/%s";

    mapsDirectory = gameDirectory.newFolder("maps").toPath();

    when(preferencesService.getCacheDirectory()).thenReturn(cacheDirectory.getRoot().toPath());
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(forgedAlliancePrefs.getCustomMapsDirectory()).thenReturn(customMapsDirectory.getRoot().toPath());
    when(forgedAlliancePrefs.customMapsDirectoryProperty()).thenReturn(customMapsDirectoryProperty);
    when(forgedAlliancePrefs.getPath()).thenReturn(gameDirectory.getRoot().toPath());
    when(preferencesService.getPreferences().getForgedAlliance().pathProperty()).thenReturn(new SimpleObjectProperty<>());

    doAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      CompletableTask<Void> task = invocation.getArgumentAt(0, CompletableTask.class);
      WaitForAsyncUtils.asyncFx(task);
      task.getFuture().get();
      return task;
    }).when(taskService).submitTask(any());

    instance.postConstruct();
  }

  @Test
  public void testGetLocalMapsNoMaps() throws Exception {
    assertThat(instance.getInstalledMaps(), hasSize(0));
  }

  @Test
  public void testGetLocalMapsOfficialMap() throws Exception {
    Path scmp001 = Files.createDirectory(mapsDirectory.resolve("SCMP_001"));
    Files.copy(getClass().getResourceAsStream("/maps/SCMP_001/SCMP_001_scenario.lua"), scmp001.resolve("SCMP_001_scenario.lua"));

    instance.postConstruct();

    ObservableList<MapBean> localMapBeans = instance.getInstalledMaps();
    assertThat(localMapBeans, hasSize(1));

    MapBean mapBean = localMapBeans.get(0);
    assertThat(mapBean, notNullValue());
    assertThat(mapBean.getFolderName(), is("SCMP_001"));
    assertThat(mapBean.getDisplayName(), is("Burial Mounds"));
    assertThat(mapBean.getSize(), equalTo(new MapSize(20, 20)));
  }

  @Test
  public void testReadMapOfNonFolderThrowsException() throws Exception {
    expectedException.expect(MapLoadException.class);
    expectedException.expectMessage(startsWith("Not a folder"));

    instance.readMap(mapsDirectory.resolve("something"));
  }

  @Test
  public void testReadMapInvalidMap() throws Exception {
    Path corruptMap = Files.createDirectory(mapsDirectory.resolve("corruptMap"));
    Files.write(corruptMap.resolve("corruptMap_scenario.lua"), "{\"This is invalid\", \"}".getBytes(UTF_8));

    expectedException.expect(MapLoadException.class);
    expectedException.expectCause(instanceOf(LuaError.class));

    instance.readMap(corruptMap);
  }

  @Test
  public void testReadMap() throws Exception {
    MapBean mapBean = instance.readMap(Paths.get(getClass().getResource("/maps/SCMP_001").toURI()));

    assertThat(mapBean, notNullValue());
    assertThat(mapBean.getId(), isEmptyOrNullString());
    assertThat(mapBean.getDescription(), startsWith("Initial scans of the planet"));
    assertThat(mapBean.getSize(), is(new MapSize(20, 20)));
    assertThat(mapBean.getVersion(), is(1));
    assertThat(mapBean.getFolderName(), is("SCMP_001"));
  }

  @Test
  public void testInstalledOfficialMapIgnoreCase() throws Exception {
    Path scmp001 = Files.createDirectory(mapsDirectory.resolve("SCMP_001"));
    Files.copy(getClass().getResourceAsStream("/maps/SCMP_001/SCMP_001_scenario.lua"), scmp001.resolve("SCMP_001_scenario.lua"));

    instance.postConstruct();

    assertTrue(instance.isInstalled("ScMp_001"));
  }

  @Test
  public void testloadPreview() throws Exception {
    for (PreviewSize previewSize : PreviewSize.values()) {
      Path cacheSubDir = Paths.get("maps").resolve(previewSize.folderName);
      instance.loadPreview("preview", previewSize);
      verify(assetService).loadAndCacheImage(any(URL.class), eq(cacheSubDir), isNull(URL.class));
    }
  }
}
