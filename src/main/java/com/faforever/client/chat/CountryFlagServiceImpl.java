package com.faforever.client.chat;

import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class CountryFlagServiceImpl implements CountryFlagService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  @Cacheable("countryFlags")
  public Image loadCountryFlag(String country) {
    String path = "/images/flags/" + country.toLowerCase() + ".png";
    try {
      return new Image(new ClassPathResource(path).getURL().toString(), true);
    } catch (IOException e) {
      logger.warn("Could not load country flag", e);
      return null;
    }
  }
}
