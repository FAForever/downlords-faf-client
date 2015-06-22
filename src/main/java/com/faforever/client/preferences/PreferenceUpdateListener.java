package com.faforever.client.preferences;

public interface PreferenceUpdateListener {

  /**
   * Called whenever the preference file has been updated.
   *
   * @param preferences
   */
  void onPreferencesUpdated(Preferences preferences);
}
