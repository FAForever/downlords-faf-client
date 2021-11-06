package com.faforever.client.player;

import com.faforever.client.exception.AssetLoadException;
import com.faforever.client.i18n.I18n;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.faforever.client.config.CacheNames.COUNTRY_FLAGS;
import static com.faforever.client.config.CacheNames.COUNTRY_NAMES;

@Slf4j
@Lazy
@Service
@RequiredArgsConstructor
public class CountryFlagService {

  private static final Collection<String> NON_COUNTRY_CODES = Arrays.asList("A1", "A2", "");
  private final I18n i18n;

  @Cacheable(value = COUNTRY_FLAGS, sync = true)
  public Optional<Image> loadCountryFlag(final String country) {
    if (country == null) {
      return Optional.empty();
    }

    return getCountryFlagUrl(country)
        .map(url -> new Image(url.toString(), true));
  }

  @Cacheable(value = COUNTRY_NAMES, sync = true)
  public List<String> getCountries(String startsWith) {
    if (startsWith == null) {
      return Arrays.stream(Locale.getISOCountries()).collect(Collectors.toList());
    }

    final String startsWithLowered = startsWith.toLowerCase();
    return Arrays.stream(Locale.getISOCountries())
        .filter(country -> matchCountry(country, startsWithLowered))
        .collect(Collectors.toList());
  }

  private boolean matchCountry(String countryCode, String startsWithLowered) {
    if (countryCode.toLowerCase().startsWith(startsWithLowered)) {
      return true;
    }

    String localName = i18n.getCountryNameLocalized(countryCode).toLowerCase();
    return localName.startsWith(startsWithLowered);
  }

  public Optional<URL> getCountryFlagUrl(String country) {
    if (country == null) {
      return Optional.empty();
    }
    String imageName;
    if (NON_COUNTRY_CODES.contains(country)) {
      imageName = "earth";
    } else {
      imageName = country.toLowerCase();
    }

    String path = "/images/flags/" + imageName + ".png";
    ClassPathResource classPathResource = new ClassPathResource(path);
    if (!classPathResource.exists()) {
      return Optional.empty();
    }

    try {
      return Optional.of(classPathResource.getURL());
    } catch (IOException e) {
      throw new AssetLoadException("Could not open classpath resource " + classPathResource.getPath(), e, "flag.loadError", country);
    }
  }
}
