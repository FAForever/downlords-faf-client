package com.faforever.client.preferences;

public interface PreferenceUpdateListener {

  /**
   * Called whenever the preference file has been updated.
   */
  void onPreferencesUpdated(Preferences preferences);
}
