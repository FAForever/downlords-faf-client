package com.faforever.client.game;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.mod.MountInfo;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class FaInitGeneratorTest extends ServiceTest {
  @TempDir
  public Path mountBaseDir;
  @TempDir
  public Path fafBinDirectory;
  @TempDir
  public Path faDirectory;
  private FaInitGenerator instance;
  @Mock
  private PreferencesService preferencesService;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new FaInitGenerator(preferencesService);

    Preferences preferences = PreferencesBuilder.create().defaultValues()
        .forgedAlliancePrefs()
        .installationPath(faDirectory)
        .then()
        .get();

    when(preferencesService.getFafBinDirectory()).thenReturn(fafBinDirectory);
    when(preferencesService.getPreferences()).thenReturn(preferences);
  }

  @Test
  public void testGenerateInitFile() throws Exception {
    Path mountBasePath = this.mountBaseDir;
    List<MountInfo> mountPaths = Arrays.asList(
        new MountInfo(mountBasePath, Path.of("foobar"), "/foobar"),
        new MountInfo(mountBasePath, Path.of("gamedata/effects.nxt"), "/effects")
    );

    instance.generateInitFile(mountPaths, new HashSet<>(Arrays.asList("/schook", "/labwars")));

    Path targetFile = fafBinDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME);
    assertTrue(Files.exists(targetFile));

    String fileContent = new String(Files.readAllBytes(targetFile), UTF_8);
    String basePathString = mountBasePath.toAbsolutePath().toString().replaceAll("[/\\\\]", "\\\\\\\\");
    assertThat(fileContent, CoreMatchers.containsString("\r\n" +
        "mountSpecs = {\r\n" +
        "    {'" + basePathString + "\\\\foobar', '/foobar'},\r\n" +
        "    {'" + basePathString + "\\\\gamedata\\\\effects.nxt', '/effects'}\r\n" +
        "}"
    ));
    assertThat(fileContent, CoreMatchers.containsString("\r\n" +
        "hook = {\r\n" +
        "    '/labwars',\r\n" +
        "    '/schook'\r\n" +
        "}"
    ));
  }
}
