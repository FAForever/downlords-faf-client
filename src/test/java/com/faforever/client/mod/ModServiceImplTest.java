package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.io.ByteCopier;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.store.RAMDirectory;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class ModServiceImplTest extends AbstractPlainJavaFxTest {

  public static final String BLACK_OPS_UNLEASHED_DIRECTORY_NAME = "BlackOpsUnleashed";
  private static final ClassPathResource BLACKOPS_SUPPORT_MOD_INFO = new ClassPathResource("/mods/blackops_support_mod_info.lua");
  private static final ClassPathResource BLACKOPS_UNLEASHED_MOD_INFO = new ClassPathResource("/mods/blackops_unleashed_mod_info.lua");
  private static final ClassPathResource ECO_MANAGER_MOD_INFO = new ClassPathResource("/mods/eco_manager_mod_info.lua");
  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

  @Rule
  public TemporaryFolder modsDirectory = new TemporaryFolder();
  @Rule
  public TemporaryFolder faDataDirectory = new TemporaryFolder();
  @Rule
  public TemporaryFolder corruptedModsDirectory = new TemporaryFolder();

  @Mock
  PreferencesService preferencesService;
  @Mock
  private Preferences preferences;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private TaskService taskService;
  @Mock
  private FafService fafService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;

  private ModServiceImpl instance;
  private Path gamePrefsPath;

  @Before
  public void setUp() throws Exception {
    instance = new ModServiceImpl();
    instance.i18n = i18n;
    instance.preferencesService = preferencesService;
    instance.applicationContext = applicationContext;
    instance.taskService = taskService;
    instance.fafService = fafService;
    instance.notificationService = notificationService;
    instance.directory = new RAMDirectory();
    instance.analyzer = new SimpleAnalyzer();

    gamePrefsPath = faDataDirectory.getRoot().toPath().resolve("game.prefs");

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(forgedAlliancePrefs.getPreferencesFile()).thenReturn(gamePrefsPath);
    when(forgedAlliancePrefs.getModsDirectory()).thenReturn(modsDirectory.getRoot().toPath());
    when(forgedAlliancePrefs.modsDirectoryProperty()).thenReturn(new SimpleObjectProperty<>(modsDirectory.getRoot().toPath()));
    // FIXME how did that happen... I see this line many times but it doesn't seem to do anything useful
    doAnswer(invocation -> invocation.getArgumentAt(0, Object.class)).when(taskService).submitTask(any());

    copyMod(BLACK_OPS_UNLEASHED_DIRECTORY_NAME, BLACKOPS_UNLEASHED_MOD_INFO);

    instance.postConstruct();
  }

  private void copyMod(String directoryName, ClassPathResource classPathResource) throws IOException {
    Path targetDir = modsDirectory.getRoot().toPath().resolve(directoryName);
    Files.createDirectories(targetDir);

    try (InputStream inputStream = classPathResource.getInputStream();
         OutputStream outputStream = Files.newOutputStream(targetDir.resolve("mod_info.lua"))) {
      ByteCopier.from(inputStream)
          .to(outputStream)
          .copy();
    }
  }

  @Test
  public void testPostConstructLoadInstalledMods() throws Exception {
    ObservableList<ModInfoBean> installedMods = instance.getInstalledMods();

    assertThat(installedMods.size(), is(1));
  }

  @Test
  public void testLoadInstalledModsLoadsMods() throws Exception {
    assertThat(instance.getInstalledMods().size(), is(1));
    copyMod("BlackopsSupport", BLACKOPS_SUPPORT_MOD_INFO);
    assertThat(instance.getInstalledMods().size(), is(1));
    instance.loadInstalledMods();
    assertThat(instance.getInstalledMods().size(), is(2));
  }


  @Test
  public void testLoadInstalledModsDoesntUnloadsMods() throws Exception {
    assertThat(instance.getInstalledMods().size(), is(1));
    copyMod("BlackopsSupport", BLACKOPS_SUPPORT_MOD_INFO);
    instance.loadInstalledMods();
    assertThat(instance.getInstalledMods().size(), is(2));

    Path modDirectory = modsDirectory.getRoot().toPath().resolve("BlackOpsUnleashed");
    Files.delete(modDirectory.resolve("mod_info.lua"));
    Files.delete(modDirectory);

    assertThat(instance.getInstalledMods().size(), is(2));
  }

  @Test
  public void testDownloadAndInstallMod() throws Exception {
    assertThat(instance.getInstalledMods().size(), is(1));

    InstallModTask task = mock(InstallModTask.class, withSettings().useConstructor());
    when(task.getFuture()).thenReturn(completedFuture(null));
    when(applicationContext.getBean(InstallModTask.class)).thenReturn(task);

    URL modUrl = new URL("http://example.com/some/mod.zip");

    copyMod("BlackopsSupport", BLACKOPS_SUPPORT_MOD_INFO);
    assertThat(instance.getInstalledMods().size(), is(1));

    instance.downloadAndInstallMod(modUrl).toCompletableFuture().get(TIMEOUT, TIMEOUT_UNIT);

    verify(task).setUrl(modUrl);
    assertThat(instance.getInstalledMods().size(), is(2));
  }

  @Test
  public void testDownloadAndInstallModWithProperties() throws Exception {
    assertThat(instance.getInstalledMods().size(), is(1));

    InstallModTask task = mock(InstallModTask.class, withSettings().useConstructor());
    when(task.getFuture()).thenReturn(completedFuture(null));
    when(applicationContext.getBean(InstallModTask.class)).thenReturn(task);

    URL modUrl = new URL("http://example.com/some/mod.zip");

    copyMod("BlackopsSupport", BLACKOPS_SUPPORT_MOD_INFO);
    assertThat(instance.getInstalledMods().size(), is(1));

    StringProperty stringProperty = new SimpleStringProperty();
    DoubleProperty doubleProperty = new SimpleDoubleProperty();

    instance.downloadAndInstallMod(modUrl, doubleProperty, stringProperty).toCompletableFuture().get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(stringProperty.isBound(), is(true));
    assertThat(doubleProperty.isBound(), is(true));

    verify(task).setUrl(modUrl);
    assertThat(instance.getInstalledMods().size(), is(2));
  }

  @Test
  public void testDownloadAndInstallModInfoBeanWithProperties() throws Exception {
    assertThat(instance.getInstalledMods().size(), is(1));

    InstallModTask task = mock(InstallModTask.class, withSettings().useConstructor());
    when(task.getFuture()).thenReturn(completedFuture(null));
    when(applicationContext.getBean(InstallModTask.class)).thenReturn(task);

    URL modUrl = new URL("http://example.com/some/mod.zip");

    copyMod("BlackopsSupport", BLACKOPS_SUPPORT_MOD_INFO);
    assertThat(instance.getInstalledMods().size(), is(1));

    StringProperty stringProperty = new SimpleStringProperty();
    DoubleProperty doubleProperty = new SimpleDoubleProperty();

    ModInfoBean modInfoBean = ModInfoBeanBuilder.create().defaultValues().downloadUrl(modUrl).get();
    instance.downloadAndInstallMod(modInfoBean, doubleProperty, stringProperty).toCompletableFuture().get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(stringProperty.isBound(), is(true));
    assertThat(doubleProperty.isBound(), is(true));

    verify(task).setUrl(modUrl);
    assertThat(instance.getInstalledMods().size(), is(2));
  }

  @Test
  public void testGetInstalledModUids() throws Exception {
    copyMod("BlackopsSupport", BLACKOPS_SUPPORT_MOD_INFO);
    instance.loadInstalledMods();

    Set<String> installedModUids = instance.getInstalledModUids();

    assertThat(installedModUids, containsInAnyOrder("9e8ea941-c306-4751-b367-f00000000005", "9e8ea941-c306-4751-b367-a11000000502"));
  }

  @Test
  public void testGetInstalledUiModsUids() throws Exception {
    assertThat(instance.getInstalledMods().size(), is(1));

    copyMod("EM", ECO_MANAGER_MOD_INFO);

    instance.loadInstalledMods();
    assertThat(instance.getInstalledMods().size(), is(2));

    Set<String> installedUiModsUids = instance.getInstalledUiModsUids();

    assertThat(installedUiModsUids, contains("b2cde810-15d0-4bfa-af66-ec2d6ecd561b"));
  }

  @Test
  public void testEnableSimModsClean() throws Exception {
    Files.createFile(gamePrefsPath);

    HashSet<String> simMods = new HashSet<>();
    simMods.add("9e8ea941-c306-4751-b367-f00000000005");
    simMods.add("9e8ea941-c306-4751-b367-a11000000502");
    instance.enableSimMods(simMods);

    List<String> lines = Files.readAllLines(gamePrefsPath);

    assertThat(lines, contains(
        "active_mods = {",
        "    ['9e8ea941-c306-4751-b367-f00000000005'] = true,",
        "    ['9e8ea941-c306-4751-b367-a11000000502'] = true",
        "}"
    ));
  }

  @Test
  public void testEnableSimModsModDisableUnselectedMods() throws Exception {
    Iterable<? extends CharSequence> lines = Arrays.asList(
        "active_mods = {",
        "    ['9e8ea941-c306-4751-b367-f00000000005'] = true,",
        "    ['9e8ea941-c306-4751-b367-a11000000502'] = true",
        "}"
    );
    Files.write(gamePrefsPath, lines);

    HashSet<String> simMods = new HashSet<>();
    simMods.add("9e8ea941-c306-4751-b367-a11000000502");
    instance.enableSimMods(simMods);

    lines = Files.readAllLines(gamePrefsPath);

    assertThat(lines, contains(
        "active_mods = {",
        "    ['9e8ea941-c306-4751-b367-a11000000502'] = true",
        "}"
    ));
  }

  @Test
  public void testExtractModInfo() throws Exception {
    copyMod("BlackopsSupport", BLACKOPS_SUPPORT_MOD_INFO);
    copyMod("EM", ECO_MANAGER_MOD_INFO);
    instance.loadInstalledMods();

    ArrayList<ModInfoBean> installedMods = new ArrayList<>(instance.getInstalledMods());
    Collections.sort(installedMods, (lhs, rhs) -> lhs.getName().compareTo(rhs.getName()));

    ModInfoBean modInfoBean = installedMods.get(0);

    assertThat(modInfoBean.getName(), is("BlackOps Global Icon Support Mod"));
    assertThat(modInfoBean.getVersion(), is("5"));
    assertThat(modInfoBean.getAuthor(), is("Exavier Macbeth, DeadMG"));
    assertThat(modInfoBean.getDescription(), is("Version 5.0. This mod provides global icon support for any mod that places their icons in the proper folder structure. See Readme"));
    assertThat(modInfoBean.getImagePath(), nullValue());
    assertThat(modInfoBean.getSelectable(), is(true));
    assertThat(modInfoBean.getId(), is("9e8ea941-c306-4751-b367-f00000000005"));
    assertThat(modInfoBean.getUiOnly(), is(false));

    modInfoBean = installedMods.get(1);

    assertThat(modInfoBean.getName(), is("BlackOps Unleashed"));
    assertThat(modInfoBean.getVersion(), is("8"));
    assertThat(modInfoBean.getAuthor(), is("Lt_hawkeye"));
    assertThat(modInfoBean.getDescription(), is("Version 5.2. BlackOps Unleased Unitpack contains several new units and game changes. Have fun"));
    assertThat(modInfoBean.getImagePath(), is(modsDirectory.getRoot().toPath().resolve("BlackOpsUnleashed/icons/yoda_icon.bmp")));
    assertThat(modInfoBean.getSelectable(), is(true));
    assertThat(modInfoBean.getId(), is("9e8ea941-c306-4751-b367-a11000000502"));
    assertThat(modInfoBean.getUiOnly(), is(false));

    modInfoBean = installedMods.get(2);

    assertThat(modInfoBean.getName(), is("EcoManager"));
    assertThat(modInfoBean.getVersion(), is("3"));
    assertThat(modInfoBean.getAuthor(), is("Crotalus"));
    assertThat(modInfoBean.getDescription(), is("EcoManager v3, more efficient energy throttling"));
    assertThat(modInfoBean.getImagePath(), nullValue());
    assertThat(modInfoBean.getSelectable(), is(true));
    assertThat(modInfoBean.getId(), is("b2cde810-15d0-4bfa-af66-ec2d6ecd561b"));
    assertThat(modInfoBean.getUiOnly(), is(true));
  }

  @Test
  public void testLoadInstalledModWithoutModInfo() throws Exception {
    Files.createDirectories(modsDirectory.getRoot().toPath().resolve("foobar"));

    assertThat(instance.getInstalledMods().size(), is(1));

    instance.loadInstalledMods();

    assertThat(instance.getInstalledMods().size(), is(1));
  }

  @Test
  public void testIsModInstalled() throws Exception {
    assertThat(instance.isModInstalled("9e8ea941-c306-4751-b367-a11000000502"), is(true));
    assertThat(instance.isModInstalled("9e8ea941-c306-4751-b367-f00000000005"), is(false));
  }

  @Test
  public void testUninstallMod() throws Exception {
    UninstallModTask uninstallModTask = mock(UninstallModTask.class);
    when(applicationContext.getBean(UninstallModTask.class)).thenReturn(uninstallModTask);
    assertThat(instance.getInstalledMods(), hasSize(1));

    instance.uninstallMod(instance.getInstalledMods().get(0));

    verify(taskService).submitTask(uninstallModTask);
  }

  @Test
  public void testGetPathForMod() throws Exception {
    assertThat(instance.getInstalledMods(), hasSize(1));

    Path actual = instance.getPathForMod(instance.getInstalledMods().get(0));

    Path expected = modsDirectory.getRoot().toPath().resolve(BLACK_OPS_UNLEASHED_DIRECTORY_NAME);
    assertThat(actual, is(expected));
  }

  @Test
  public void testGetPathForModUnknownModReturnsNull() throws Exception {
    assertThat(instance.getInstalledMods(), hasSize(1));

    assertThat(instance.getPathForMod(ModInfoBeanBuilder.create().uid("1").get()), Matchers.nullValue());
  }

  @Test
  public void testUploadMod() throws Exception {
    ModUploadTask modUploadTask = mock(ModUploadTask.class);

    when(applicationContext.getBean(ModUploadTask.class)).thenReturn(modUploadTask);

    Path modPath = Paths.get(".");

    instance.uploadMod(modPath);

    verify(applicationContext).getBean(ModUploadTask.class);

    verify(modUploadTask).setModPath(modPath);

    verify(taskService).submitTask(modUploadTask);
  }
}
