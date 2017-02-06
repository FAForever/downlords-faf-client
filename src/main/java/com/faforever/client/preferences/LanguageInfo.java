package com.faforever.client.preferences;


public enum LanguageInfo {
  AUTO(null, null, "settings.languages.system"),
  EN("en", "UK", "settings.languages.english"),
  DE("de", "DE", "settings.languages.german"),
  RU("ru", "RU", "settings.languages.russian");

  private final String languageCode;
  private final String countryCode;
  private final String displayNameKey;

  LanguageInfo(String languageCode, String countryCode, String displayNameKey) {
    this.countryCode = countryCode;
    this.displayNameKey = displayNameKey;
    this.languageCode = languageCode;
  }

  public String getLanguageCode() {
    return languageCode;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public String getDisplayNameKey() {
    return displayNameKey;
  }
}
