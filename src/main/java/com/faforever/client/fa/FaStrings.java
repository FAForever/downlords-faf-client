package com.faforever.client.fa;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FaStrings {

  public String removeLocalizationTag(String description) {
    return description.replaceAll("<LOC .*?>", "");
  }
}
