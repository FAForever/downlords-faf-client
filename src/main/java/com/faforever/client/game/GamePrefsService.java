package com.faforever.client.game;

import com.faforever.client.exception.AssetLoadException;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.US_ASCII;

@Lazy
@Slf4j
@Service
@RequiredArgsConstructor
public class GamePrefsService implements InitializingBean {

  private static final Pattern ACTIVE_MODS_PATTERN = Pattern.compile("active_mods\\s*=\\s*\\{.*?}", Pattern.DOTALL);
  private static final Pattern ACTIVE_MOD_PATTERN = Pattern.compile("\\['(.*?)']\\s*=\\s*(true|false)", Pattern.DOTALL);

  private static final Pattern GAME_PREFS_ALLOW_MULTI_LAUNCH_PATTERN = Pattern.compile(
      "debug\\s*=(\\s)*[{][^}]*enable_debug_facilities\\s*=\\s*true");
  private static final String GAME_PREFS_ALLOW_MULTI_LAUNCH_STRING = """

      debug = {
          enable_debug_facilities = true
      }""".trim();

  private final ForgedAlliancePrefs forgedAlliancePrefs;

  @Override
  public void afterPropertiesSet() throws Exception {
    patchGamePrefsForMultiInstances().exceptionally(throwable -> {
      log.error("Game.prefs patch failed", throwable);
      return null;
    });
  }

  public Set<String> readActiveModUIDs() {
    Set<String> activeMods = new HashSet<>();
    String preferencesContent = readPreferencesFile();
    Matcher matcher = ACTIVE_MODS_PATTERN.matcher(preferencesContent);
    if (matcher.find()) {
      Matcher activeModMatcher = ACTIVE_MOD_PATTERN.matcher(matcher.group(0));
      while (activeModMatcher.find()) {
        String modUid = activeModMatcher.group(1);
        if (Boolean.parseBoolean(activeModMatcher.group(2))) {
          activeMods.add(modUid);
        }
      }
    }
    return activeMods;
  }

  public void writeActiveModUIDs(Set<String> activeMods) throws IOException {
    Path preferencesFile = forgedAlliancePrefs.getPreferencesFile();
    String preferencesContent = readPreferencesFile();
    String currentActiveModsContent = null;
    Matcher matcher = ACTIVE_MODS_PATTERN.matcher(preferencesContent);
    if (matcher.find()) {
      currentActiveModsContent = matcher.group(0);
    }

    String newActiveModsContent = "active_mods = {\n%s\n}".formatted(
        activeMods.stream().map("    ['%s'] = true"::formatted).collect(Collectors.joining(",\n")));

    if (currentActiveModsContent != null) {
      preferencesContent = preferencesContent.replace(currentActiveModsContent, newActiveModsContent);
    } else {
      preferencesContent += newActiveModsContent;
    }

    Files.writeString(preferencesFile, preferencesContent);
  }

  public CompletableFuture<Void> patchGamePrefsForMultiInstances() {
    return CompletableFuture.runAsync(() -> {
      String prefsContent = readPreferencesFile();

      if (GAME_PREFS_ALLOW_MULTI_LAUNCH_PATTERN.matcher(prefsContent).find()) {
        log.debug("game.prefs file already patched to allow multiple instances");
        return;
      }

      Path preferencesFile = forgedAlliancePrefs.getPreferencesFile();
      try {
        Files.writeString(preferencesFile, GAME_PREFS_ALLOW_MULTI_LAUNCH_STRING, US_ASCII, StandardOpenOption.APPEND);
      } catch (IOException exception) {
        throw new IllegalStateException("Unable to patch game prefs file");
      }
    });
  }

  private String readPreferencesFile() {
    Map<String, Charset> availableCharsets = Charset.availableCharsets();
    String preferencesContent = null;
    for (Charset charset : availableCharsets.values()) {
      log.info("Trying to read preferences file with charset: " + charset.displayName());
      try {
        preferencesContent = Files.readString(forgedAlliancePrefs.getPreferencesFile(), charset);
        log.info("Successfully read preferences file with charset: " + charset.displayName());
        break;
      } catch (IOException e) {
        log.info("Failed to read preferences file with charset: " + charset.displayName());
        // Continue and try a different character set
      } catch (Exception e) {
        // Handle all other exceptions
        log.error("An unexpected error occurred while reading the preferences file", e);
        throw new RuntimeException(e);
      }
    }
    if (preferencesContent == null) {
      throw new AssetLoadException("Could not read preferences file", null, "file.errorReadingPreferences");
    }
    return preferencesContent;
  }
}
