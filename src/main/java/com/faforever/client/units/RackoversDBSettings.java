package com.faforever.client.units;

import lombok.Data;

import java.util.Locale;

@Data
public class RackoversDBSettings {
  private final String[] showArmies = {"Aeon", "UEF", "Cybran", "Seraphim"};
  private final String previewCorner = "None";
  private final String spookyMode = "0";
  private final String lang;

  public RackoversDBSettings(Locale locale) {
    switch (locale.getLanguage()) {
      case "cs":
        lang = "CZ";
        break;
      case "de":
        lang = "DE";
        break;
      case "es":
        lang = "ES";
        break;
      case "fr":
        lang = "FR";
        break;
      case "it":
        lang = "IT";
        break;
      case "pl":
        lang = "PL";
        break;
      case "ru":
        lang = "RU";
        break;
      case "tzm":
        lang = "TZM";
        break;
      default:
        lang = "US";
    }
  }
}
