package com.faforever.client.chat;

import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;

import static com.faforever.client.config.CacheKeys.COUNTRY_FLAGS;

public class CountryFlagServiceImpl implements CountryFlagService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final Collection<String> NON_COUNTRY_CODES = Arrays.asList("A1", "A2", "");

  @Override
  @Cacheable(COUNTRY_FLAGS)
  public Image loadCountryFlag(final String country) {
    if (country == null) {
      return null;
    }

    String imageName;
    if (NON_COUNTRY_CODES.contains(country)) {
      imageName = "earth";
    } else {
      imageName = country.toLowerCase();
    }

    String path = "/images/flags/" + imageName + ".png";
    try {
      return new Image(new ClassPathResource(path).getURL().toString(), true);
    } catch (IOException e) {
      logger.warn("Could not load country flag", e);
      return null;
    }
  }
}
