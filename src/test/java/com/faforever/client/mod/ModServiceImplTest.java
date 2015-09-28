package com.faforever.client.mod;

import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.util.ByteCopier;
import javafx.collections.ObservableList;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class ModServiceImplTest extends AbstractPlainJavaFxTest {

  private static final ClassPathResource BLACKOPS_SUPPORT_MOD_INFO = new ClassPathResource("/mods/blackops_support_mod_info.lua");
  private static final ClassPathResource BLACKOPS_UNLEASHED_MOD_INFO = new ClassPathResource("/mods/blackops_unleashed_mod_info.lua");
  private static final ClassPathResource ECO_MANAGER_MOD_INFO = new ClassPathResource("/mods/eco_manager_mod_info.lua");
  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

  @Rule
  public TemporaryFolder modsDirectory = new TemporaryFolder();
  @Rule
  public TemporaryFolder faDataDirectory = new TemporaryFolder();

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

  private ModServiceImpl instance;
  private Path gamePrefsPath;

  @Before
  public void setUp() throws Exception {
    instance = new ModServiceImpl();
    instance.preferencesService = preferencesService;
    instance.applicationContext = applicationContext;
    instance.taskService = taskService;

    gamePrefsPath = faDataDirectory.getRoot().toPath().resolve("game.prefs");

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(forgedAlliancePrefs.getModsDirectory()).thenReturn(modsDirectory.getRoot().toPath());
    when(forgedAlliancePrefs.getPreferencesFile()).thenReturn(gamePrefsPath);

    copyMod("BlackOpsUnleashed", BLACKOPS_UNLEASHED_MOD_INFO);

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

    DownloadModTask task = mock(DownloadModTask.class, withSettings().useConstructor());
    when(applicationContext.getBean(DownloadModTask.class)).thenReturn(task);
    when(taskService.submitTask(task)).thenReturn(CompletableFuture.completedFuture(null));

    String modPath = "some/mod.zip";

    copyMod("BlackopsSupport", BLACKOPS_SUPPORT_MOD_INFO);
    assertThat(instance.getInstalledMods().size(), is(1));

    instance.downloadAndInstallMod(modPath).get(TIMEOUT, TIMEOUT_UNIT);

    verify(task).setModPath(modPath);
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
    copyMod("EM", ECO_MANAGER_MOD_INFO);
    instance.loadInstalledMods();

    ObservableList<ModInfoBean> installedMods = instance.getInstalledMods();
    ModInfoBean modInfoBean = installedMods.get(0);

    assertThat(modInfoBean.getName(), is("BlackOps Unleashed"));
    assertThat(modInfoBean.getVersion(), is("8"));
    assertThat(modInfoBean.getAuthor(), is("Lt_hawkeye"));
    assertThat(modInfoBean.getDescription(), is("Version 5.2. BlackOps Unleased Unitpack contains several new units and game changes. Have fun"));
    assertThat(modInfoBean.getImagePath(), is(modsDirectory.getRoot().toPath().resolve("BlackOpsUnleashed/icons/yoda_icon.bmp")));
    assertThat(modInfoBean.getSelectable(), is(true));
    assertThat(modInfoBean.getUid(), is("9e8ea941-c306-4751-b367-a11000000502"));
    assertThat(modInfoBean.getUiOnly(), is(false));

    modInfoBean = installedMods.get(1);

    assertThat(modInfoBean.getName(), is("EcoManager"));
    assertThat(modInfoBean.getVersion(), is("3"));
    assertThat(modInfoBean.getAuthor(), is("Crotalus"));
    assertThat(modInfoBean.getDescription(), is("EcoManager v3, more efficient energy throttling"));
    assertThat(modInfoBean.getImagePath(), nullValue());
    assertThat(modInfoBean.getSelectable(), is(true));
    assertThat(modInfoBean.getUid(), is("b2cde810-15d0-4bfa-af66-ec2d6ecd561b"));
    assertThat(modInfoBean.getUiOnly(), is(true));
  }

  @Test
  public void testLoadInstalledModWithoutModInfo() throws Exception {
    Files.createDirectories(modsDirectory.getRoot().toPath().resolve("foobar"));

    assertThat(instance.getInstalledMods().size(), is(1));

    instance.loadInstalledMods();

    assertThat(instance.getInstalledMods().size(), is(1));
  }
}
