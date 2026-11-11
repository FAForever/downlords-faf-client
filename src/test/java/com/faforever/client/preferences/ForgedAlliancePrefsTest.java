package com.faforever.client.preferences;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ForgedAlliancePrefsTest {

  /** As PreferencesConfig writes and reads client.prefs: empty values left out, unknown ones ignored. */
  private final ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(Include.NON_EMPTY)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  @Test
  public void theNativeNetworkSettingTurnedOffStaysOffAfterSavingAndLoading() throws Exception {
    ForgedAlliancePrefs prefs = new ForgedAlliancePrefs();
    // The client always has these; the maps and mods folders are read from the vault's.
    prefs.setVaultBaseDirectory(java.nio.file.Path.of("vault"));
    prefs.setInstallationPath(java.nio.file.Path.of("game"));
    prefs.setPreferencesFile(java.nio.file.Path.of("Game.prefs"));
    prefs.setUseNativeNetwork(false);

    String saved = objectMapper.writeValueAsString(prefs);

    assertFalse(objectMapper.readValue(saved, ForgedAlliancePrefs.class).isUseNativeNetwork());
  }

  @Test
  public void theSettingsPropertiesFollowTheirValues() {
    ForgedAlliancePrefs prefs = new ForgedAlliancePrefs();
    prefs.setUseNativeNetwork(false);
    prefs.setSendDesyncReports(true);
    prefs.setAskedSendDesyncReports(true);

    assertFalse(prefs.useNativeNetworkProperty().get());
    assertTrue(prefs.sendDesyncReportsProperty().get());
    assertTrue(prefs.askedSendDesyncReportsProperty().get());
  }

  @Test
  public void theNativeNetworkSettingIsOnWhenPrefsDontNameIt() throws Exception {
    assertTrue(objectMapper.readValue("{}", ForgedAlliancePrefs.class).isUseNativeNetwork());
  }
}
