package com.faforever.client.game;

import com.faforever.client.patch.MountPoint;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class FaInitGeneratorTest {
  @Rule
  public TemporaryFolder folderToMount = new TemporaryFolder();
  @Rule
  public TemporaryFolder fafBinDirectory = new TemporaryFolder();
  @Rule
  public TemporaryFolder faDirectory = new TemporaryFolder();
  private FaInitGenerator instance;
  @Mock
  private PreferencesService preferencesService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new FaInitGenerator(preferencesService);

    Preferences preferences = new Preferences();
    preferences.getForgedAlliance().setPath(faDirectory.getRoot().toPath());

    when(preferencesService.getFafBinDirectory()).thenReturn(fafBinDirectory.getRoot().toPath());
    when(preferencesService.getPreferences()).thenReturn(preferences);
  }

  @Test
  public void testGenerateInitFile() throws Exception {
    Path pathToMount = folderToMount.getRoot().toPath();
    List<MountPoint> mountPaths = Arrays.asList(
        new MountPoint(pathToMount, "/"),
        new MountPoint(Paths.get("gamedata", "effects.nxt"), "/effects")
    );

    instance.generateInitFile(mountPaths, new HashSet<>(Arrays.asList("/schook", "/labwars")));

    Path targetFile = fafBinDirectory.getRoot().toPath().resolve(ForgedAlliancePrefs.INIT_FILE_NAME);
    assertTrue(Files.exists(targetFile));

    String fileContent = new String(Files.readAllBytes(targetFile), UTF_8);
    assertThat(fileContent, CoreMatchers.containsString("-- Generated\r\n" +
        "mountSpecs = {\r\n" +
        "    {'/', '" + pathToMount.toAbsolutePath().toString().replaceAll("[/\\\\]", "\\\\\\\\") + "'},\r\n" +
        "    {'/effects', '" + Paths.get("gamedata", "effects.nxt").toString().replaceAll("[/\\\\]", "\\\\\\\\") + "'}\r\n" +
        "}"
    ));
    assertThat(fileContent, CoreMatchers.containsString("-- Generated\r\n" +
        "hook = {\r\n" +
        "    '/labwars',\r\n" +
        "    '/schook'\r\n" +
        "}"
    ));
  }
}
