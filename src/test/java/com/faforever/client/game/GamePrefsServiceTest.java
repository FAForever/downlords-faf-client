package com.faforever.client.game;

import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Spy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

class GamePrefsServiceTest extends ServiceTest {

  @InjectMocks
  private GamePrefsService instance;

  @Spy
  private ForgedAlliancePrefs forgedAlliancePrefs;

  @TempDir
  public Path tempDirectory;

  private Path gamePrefsPath;

  @BeforeEach
  public void setUp() throws Exception {
    gamePrefsPath = tempDirectory.resolve("game.prefs");

    forgedAlliancePrefs.setPreferencesFile(gamePrefsPath);
  }

  @Test
  public void testEnableSimModsClean() throws Exception {
    Files.createFile(gamePrefsPath);

    HashSet<String> simMods = new HashSet<>();
    simMods.add("9e8ea941-c306-4751-b367-f00000000005");
    simMods.add("9e8ea941-c306-4751-b367-a11000000502");
    instance.writeActiveModUIDs(simMods);

    List<String> lines = Files.readAllLines(gamePrefsPath);

    assertThat(lines, contains("active_mods = {", "    ['9e8ea941-c306-4751-b367-f00000000005'] = true,",
                               "    ['9e8ea941-c306-4751-b367-a11000000502'] = true", "}"));
  }

  @Test
  public void testEnableSimModsModDisableUnselectedMods() throws Exception {
    Iterable<? extends CharSequence> lines = Arrays.asList("active_mods = {",
                                                           "    ['9e8ea941-c306-4751-b367-f00000000005'] = true,",
                                                           "    ['9e8ea941-c306-4751-b367-a11000000502'] = true", "}");
    Files.write(gamePrefsPath, lines);

    HashSet<String> simMods = new HashSet<>();
    simMods.add("9e8ea941-c306-4751-b367-a11000000502");
    instance.writeActiveModUIDs(simMods);

    lines = Files.readAllLines(gamePrefsPath);

    assertThat(lines, contains("active_mods = {", "    ['9e8ea941-c306-4751-b367-a11000000502'] = true", "}"));
  }

}