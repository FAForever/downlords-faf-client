package com.faforever.client.map;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.update.ClientConfiguration;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.luaj.vm2.LuaError;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.testfx.util.WaitForAsyncUtils;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapServiceTest extends AbstractPlainJavaFxTest {

  @Rule
  public TemporaryFolder cacheDirectory = new TemporaryFolder();
  @Rule
  public TemporaryFolder customMapsDirectory = new TemporaryFolder();
  @Rule
  public TemporaryFolder gameDirectory = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MapService instance;
  private Path mapsDirectory;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private Preferences preferences;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Mock
  private ObjectProperty<Path> customMapsDirectoryProperty;
  @Mock
  private TaskService taskService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private AssetService assetService;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private FafService fafService;
  @Mock
  private MapGeneratorService mapGeneratorService;
  @Mock
  private EventBus eventBus;

  @Before
  public void setUp() throws Exception {
    ClientProperties clientProperties = new ClientProperties();
    clientProperties.getVault().setMapPreviewUrlFormat("http://127.0.0.1:65534/preview/%s/%s");

    mapsDirectory = gameDirectory.newFolder("maps").toPath();
    when(forgedAlliancePrefs.getCustomMapsDirectory()).thenReturn(customMapsDirectory.getRoot().toPath());
    when(forgedAlliancePrefs.customMapsDirectoryProperty()).thenReturn(customMapsDirectoryProperty);
    when(forgedAlliancePrefs.getInstallationPath()).thenReturn(gameDirectory.getRoot().toPath());
    when(forgedAlliancePrefs.installationPathProperty()).thenReturn(new SimpleObjectProperty<>());
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    instance = new MapService(preferencesService, taskService, applicationContext,
        fafService, assetService, i18n, uiService, mapGeneratorService, clientProperties, eventBus);
    instance.afterPropertiesSet();

    doAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      CompletableTask<Void> task = invocation.getArgument(0);
      WaitForAsyncUtils.asyncFx(task);
      task.getFuture().get();
      return task;
    }).when(taskService).submitTask(any());

    instance.officialMaps = ImmutableSet.of();
    instance.afterPropertiesSet();
  }

  @Test
  public void testGetLocalMapsNoMaps() {
    assertThat(instance.getInstalledMaps(), hasSize(0));
  }

  @Test
  public void testGetLocalMapsOfficialMap() throws Exception {
    instance.officialMaps = ImmutableSet.of("SCMP_001");
    
    Path scmp001 = Files.createDirectory(mapsDirectory.resolve("SCMP_001"));
    Files.copy(getClass().getResourceAsStream("/maps/SCMP_001/SCMP_001_scenario.lua"), scmp001.resolve("SCMP_001_scenario.lua"));

    instance.afterPropertiesSet();

    ObservableList<MapBean> localMapBeans = instance.getInstalledMaps();
    assertThat(localMapBeans, hasSize(1));

    MapBean mapBean = localMapBeans.get(0);
    assertThat(mapBean, notNullValue());
    assertThat(mapBean.getFolderName(), is("SCMP_001"));
    assertThat(mapBean.getDisplayName(), is("Burial Mounds"));
    assertThat(mapBean.getSize(), equalTo(MapSize.valueOf(1024, 1024)));
  }

  @Test
  public void testReadMapOfNonFolderThrowsException() {
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
    assertThat(mapBean.getSize(), is(MapSize.valueOf(1024, 1024)));
    assertThat(mapBean.getVersion(), is(new ComparableVersion("1")));
    assertThat(mapBean.getFolderName(), is("SCMP_001"));
  }

  @Test
  public void testInstalledOfficialMapIgnoreCase() throws Exception {
    instance.officialMaps = ImmutableSet.of("SCMP_001");
    
    Path scmp001 = Files.createDirectory(mapsDirectory.resolve("SCMP_001"));
    Files.copy(getClass().getResourceAsStream("/maps/SCMP_001/SCMP_001_scenario.lua"), scmp001.resolve("SCMP_001_scenario.lua"));

    instance.afterPropertiesSet();

    assertTrue(instance.isInstalled("ScMp_001"));
  }

  @Test
  public void testLoadPreview() {
    for (PreviewSize previewSize : PreviewSize.values()) {
      Path cacheSubDir = Paths.get("maps").resolve(previewSize.folderName);
      instance.loadPreview("preview", previewSize);
      verify(assetService).loadAndCacheImage(any(URL.class), eq(cacheSubDir), any());
    }
  }

  @Test
  public void testGetRecommendedMaps() throws Exception {
    ClientConfiguration clientConfiguration = mock(ClientConfiguration.class);
    List<Integer> recommendedMapIds = Lists.newArrayList(1, 2, 3);
    when(clientConfiguration.getRecommendedMaps()).thenReturn(recommendedMapIds);
    when(preferencesService.getRemotePreferencesAsync()).thenReturn(CompletableFuture.completedFuture(clientConfiguration));
    when(fafService.getMapsById(recommendedMapIds, 10, 0)).thenReturn(CompletableFuture.completedFuture(null));

    instance.getRecommendedMaps(10, 0);

    verify(fafService).getMapsById(recommendedMapIds, 10, 0);
  }

  @Test
  public void testGetHighestRatedMaps() {
    when(fafService.getHighestRatedMaps(10, 0)).thenReturn(CompletableFuture.completedFuture(null));
    instance.getHighestRatedMaps(10, 0);
    verify(fafService).getHighestRatedMaps(10, 0);
  }

  @Test
  public void testGetNewestMaps() {
    when(fafService.getNewestMaps(10, 0)).thenReturn(CompletableFuture.completedFuture(null));
    instance.getNewestMaps(10, 0);
    verify(fafService).getNewestMaps(10, 0);
  }

  @Test
  public void testGetMostPlayedMaps() {
    when(fafService.getMostPlayedMaps(10, 0)).thenReturn(CompletableFuture.completedFuture(null));
    instance.getMostPlayedMaps(10, 0);
    verify(fafService).getMostPlayedMaps(10, 0);
  }
}
