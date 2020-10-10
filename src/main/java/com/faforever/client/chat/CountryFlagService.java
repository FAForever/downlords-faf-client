package com.faforever.client.chat;

import javafx.scene.image.Image;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.faforever.client.config.CacheNames.COUNTRY_FLAGS;
import static com.faforever.client.config.CacheNames.COUNTRY_NAMES;

@Lazy
@Service
public class CountryFlagService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final Collection<String> NON_COUNTRY_CODES = Arrays.asList("A1", "A2", "");

  @Cacheable(COUNTRY_FLAGS)
  public Optional<Image> loadCountryFlag(final String country) {
    if (country == null) {
      return Optional.empty();
    }

    return getCountryFlagUrl(country)
        .map(url -> new Image(url.toString(), true));
  }

  @Cacheable(COUNTRY_NAMES)
  public List<String> getCountries(String startsWith) {
    String[] countries = Locale.getISOCountries();
    if(startsWith == null) {
      return Arrays.asList(countries);
    }

    startsWith = startsWith.toLowerCase();

    ArrayList<String> result = new ArrayList(countries.length);
    for(String country : countries)  {
      if(country.toLowerCase().startsWith(startsWith))
        result.add(country);
    }

    return result;
  }

  @SneakyThrows
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
    return Optional.of(classPathResource.getURL());
  }
}
